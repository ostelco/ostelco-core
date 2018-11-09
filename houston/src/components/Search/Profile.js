import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { Col, Row, Card, CardBody, CardTitle, Button } from 'reactstrap';

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
    const state = this.state;
    state.showBlock = false;
    this.setState(state);
  }

  handleShowBlock = () => {
    const state = this.state;
    state.showBlock = true;
    this.setState(state);
  }

  handleConfirmBlock = () => {
    this.handleCloseBlock();
    // TODO call the method to give additional data
  }

  handleCloseNewSIM = () => {
    const state = this.state;
    state.showNewSIM = false;
    this.setState(state);
  }

  handleShowNewSIM = () => {
    const state = this.state;
    state.showNewSIM = true;
    this.setState(state);
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
    if (!props.profile.name) return null;
    return (
      <Card>
        <CardBody>
          <CardTitle>User Profile</CardTitle>
          <Row>
            <Col xs={2} md={2}>
              <samp>{'Name:'}</samp>
            </Col>
            <Col xs={12} md={8}>
              <samp>{`${props.profile.name}`}</samp>
            </Col>
          </Row>
          <Row>
            <Col xs={2} md={2}>
              <samp>{'Email:'}</samp>
            </Col>
            <Col xs={12} md={8}>
              <samp>{`${props.profile.email}`}</samp>
            </Col>
          </Row>

          <Row>
            <Col xs={2} md={2}>
              <samp>{'Address:'}</samp>
            </Col>
            <Col xs={12} md={8}>
              <samp>{`${props.profile.address}`}</samp>
            </Col>
          </Row>
          <br />
          <Row>
            <Col xs={12} md={6}>
              <samp>
                <Button color="danger" onClick={this.handleShowBlock}>
                  {'Block current SIM card'}
                </Button>
              </samp>
            </Col>
          </Row>
          <br />
          <Row>
            <Col xs={12} md={6}>
              <samp>
                <Button onClick={this.handleShowNewSIM}>
                  {'Order new SIM card'}
                </Button>
              </samp>
            </Col>
          </Row>
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
        </CardBody>
      </Card>
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
