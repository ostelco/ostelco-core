import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { Col, Row, Card, CardBody, CardHeader } from 'reactstrap';

import WarningModal from '../Shared/WarningModal';
import { humanReadableBytes } from '../../helpers';

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
    if (!props.balance) {
      return null;
    }
    return (
      <Card>
        <CardHeader>Data balance</CardHeader>
        <CardBody>
          <Row >
            <Col xs={6} md={4}>
              {`Remaining ${props.balance}.`}
            </Col>
            {/* <Col xs={6} md={4}>
              <Button onClick={this.handleShowModal}>{'Give additional 1 GB'}</Button>
            </Col> */}
          </Row>
          <WarningModal
            heading={modalHeading}
            warningText={modalText}
            show={this.state.showWarning}
            handleConfirm={this.handleConfirmModal}
            handleClose={this.handleCloseModal} />
        </CardBody>
      </Card>
    );
  }
}

DataUsage.propTypes = {
  loggedIn: PropTypes.bool,
  pseudonym: PropTypes.object,
  balance: PropTypes.string
};

function mapStateToProps(state) {
  const { bundles } = state;
  let balance = null;
  if (Array.isArray(bundles)) {
    balance = humanReadableBytes(bundles[0].balance);
  }
  return {
    balance
  };
}

export default connect(mapStateToProps)(DataUsage);
