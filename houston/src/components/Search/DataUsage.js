import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { authActions, pseudoActions } from '../../actions';
import { Grid, Row, Col, Button, Panel } from 'react-bootstrap';
import { WarningModal } from '../Shared/WarningModal'

class DataUsage extends React.Component {
  constructor(props, context) {
    super(props, context);
    this.state = {
      showWarning: false
    };
  }

  handleCloseModal = () => {
    this.setState({
      showWarning: false
    });
  }

  handleShowModal = () => {
    this.setState({
      showWarning: true
    });
  }

  handleConfirmModal = () => {
    this.handleCloseModal();
    // TODO call the method to give additional data
  }

  render() {
    const modalHeading = `Confirm additional Data`
    const modalText = `Do you really want to give this user an additional 1 GB of Data ?`
    const props = this.props;
    if (!props.balance) return null;
    return (
      <Panel>
        <Panel.Heading>Data balance</Panel.Heading>
        <Panel.Body>
          <Grid>
            <Row className="show-grid">
              <Col xs={6} md={4}>
                <samp>{`Remaining ${props.balance}.`}</samp>
              </Col>
              <Col xs={6} md={4}>
                <samp><Button bsStyle="link" onClick={this.handleShowModal}>{'Give additional 1 GB'}</Button></samp>
              </Col>
            </Row>
          </Grid>
          <WarningModal
            heading={modalHeading}
            warningText={modalText}
            show={this.state.showWarning}
            handleConfirm = {this.handleConfirmModal}
            handleClose = {this.handleCloseModal}/>
        </Panel.Body>
      </Panel>
    );
  }
}

DataUsage.propTypes = {
  loggedIn: PropTypes.bool,
  pseudonym: PropTypes.object,
  balance: PropTypes.string
};

function humanReadableBytes(sizeInBytes) {
  var i = -1;
  var byteUnits = ['KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
  do {
    sizeInBytes = sizeInBytes / 1024;
    i++;
  } while (sizeInBytes > 1024);
  return `${Math.max(sizeInBytes, 0.1).toFixed(1)} ${byteUnits[i]}`;
}

function mapStateToProps(state) {
  const { bundles } = state;
  let balance = null;
  if (bundles.data) {
    balance = humanReadableBytes(bundles.data[0].balance);
  }
  return {
    balance
  };
}
const mapDispatchToProps = {
  login: authActions.login,
  getPseudonym: pseudoActions.getPseudonym
}
export default connect(mapStateToProps, mapDispatchToProps)(DataUsage);
