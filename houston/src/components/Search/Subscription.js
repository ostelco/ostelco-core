import React from 'react';
import PropTypes from 'prop-types';
import { Col, Row, Button } from 'reactstrap';

import WarningModal from '../Shared/WarningModal';

export default class Subscription extends React.Component {
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
    const blockHeading = 'Confirm Blocking of SIM';
    const blockText = 'Do you really want to block the current SIM card ? (Not implemented)';
    const newSIMHeading = 'Confirm new SIM';
    const newSIMText = 'Do you really want to provision new SIM card ? (Not implemented)';

    const { subscription } = this.props;
    return (
      <>
        <Row>
          <Col xs={2} md={2}>{'Phone number:'}</Col>
          <Col xs={12} md={8}>{`${subscription.msisdn}`}</Col>
        </Row>
        {/* <Row>
          <Col xs={6} md={4}>
            <Button color="danger" onClick={this.handleShowBlock}>
              {'Block current SIM card'}
            </Button>
          </Col>
          <Col xs={6} md={4}>
            <Button onClick={this.handleShowNewSIM}>
              {'Order new SIM card'}
            </Button>
          </Col>
        </Row> */}
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
      </>
    );
  }
}

Subscription.propTypes = {
  subscription: PropTypes.shape({
    msisdn: PropTypes.string,
    alias: PropTypes.string
  }),
};
