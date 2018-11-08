import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { Button, Panel, Grid, Row, Col } from 'react-bootstrap';
import WarningModal from '../Shared/WarningModal';

class Profile extends React.Component {
  constructor(props, context) {
    super(props, context);
    this.state = {
      showBlock: false,
      showNewSIM: false
    };
  }
  handleCloseBlock = () => {
    this.state.showBlock = false;
    this.setState(this.state);
  }

  handleShowBlock = () => {
    this.state.showBlock = true;
    this.setState(this.state);
  }

  handleConfirmBlock = () => {
    this.handleCloseBlock();
    // TODO call the method to give additional data
  }

  handleCloseNewSIM = () => {
    this.state.showNewSIM = false;
    this.setState(this.state);
  }

  handleShowNewSIM = () => {
    this.state.showNewSIM = true;
    this.setState(this.state);
  }

  handleConfirmNewSIM = () => {
    this.handleCloseNewSIM();
    // TODO call the method to give additional data
  }

  render() {
    const blockHeading = `Confirm Blocking of SIM`
    const blockText = `Do you really want to block the current SIM card ?`
    const newSIMHeading = `Confirm new SIM`
    const newSIMText = `Do you really want to provision new SIM card ?`

    const props = this.props;
    console.log(JSON.stringify(props.profile));
    if (!props.profile.name) return null;
    return (
      <Panel>
        <Panel.Heading>User Profile</Panel.Heading>
        <Panel.Body>
          <Grid>
            <Row className="show-grid">
              <Col xs={1} md={1}>
                <samp>{'Name:'}</samp>
              </Col>
              <Col xs={12} md={8}>
                <samp>{`${props.profile.name}`}</samp>
              </Col>
            </Row>
            <Row className="show-grid">
              <Col xs={1} md={1}>
                <samp>{'Email:'}</samp>
              </Col>
              <Col xs={12} md={8}>
                <samp>{`${props.profile.email}`}</samp>
              </Col>
            </Row>

            <Row className="show-grid">
              <Col xs={1} md={1}>
                <samp>{'Address:'}</samp>
              </Col>
              <Col xs={12} md={8}>
                <samp>{`${props.profile.address}`}</samp>
              </Col>
            </Row>
            <br />
            <Row className="show-grid">
              <Col xs={12} md={6}>
                <samp>
                  {'Block '}
                  <Button bsStyle="link" onClick={this.handleShowBlock}>
                    {'current SIM card'}
                  </Button>
                </samp>
              </Col>
            </Row>
            <Row className="show-grid">
              <Col xs={12} md={6}>
                <samp>{'Order '}
                  <Button bsStyle="link" onClick={this.handleShowNewSIM}>
                    {'new SIM card'}
                  </Button>
                </samp>
              </Col>
            </Row>
          </Grid>
          <WarningModal
            heading={blockHeading}
            warningText={blockText}
            show={this.state.showBlock}
            handleConfirm={this.handleConfirmBlock}
            handleClose={this.handleCloseBlock} />
          <WarningModal
            heading={newSIMHeading}
            warningText={newSIMText}
            show={this.state.showNewSIM}
            handleConfirm={this.handleConfirmNewSIM}
            handleClose={this.handleCloseNewSIM} />
        </Panel.Body>
      </Panel>
    );
  }
}
Profile.propTypes = {
  profile: PropTypes.shape({
    name: PropTypes.string,
    email: PropTypes.string,
    address: PropTypes.string
  }),
};

function mapStateToProps(state) {
  const { subscriber } = state;
  return {
    profile: subscriber
  };
}
export default connect(mapStateToProps)(Profile);
