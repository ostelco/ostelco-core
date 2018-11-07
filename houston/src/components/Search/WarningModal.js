import React from 'react';
import PropTypes from 'prop-types';
import { Modal, Button, Popover, OverlayTrigger, Tooltip } from 'react-bootstrap';

const WarningModal = props => {
  const popover = (
    <Popover id="modal-popover" title="popover">
      very popover. such engagement
    </Popover>
  );
  const tooltip = <Tooltip id="modal-tooltip">wow.</Tooltip>;
  return (
    <Modal show={props.show} onHide={props.handleClose}>
    <Modal.Header closeButton>
      <Modal.Title>{props.heading}</Modal.Title>
    </Modal.Header>
    <Modal.Body>
      <p>
        {props.warningText}
      </p>

    </Modal.Body>
    <Modal.Footer>
      <Button onClick={props.handleClose}>Close</Button>
      <Button onClick={props.handleConfirm} bsStyle="primary">Yes</Button>
    </Modal.Footer>
  </Modal>
  );
}

WarningModal.propTypes = {
  show: PropTypes.bool.isRequired,
  heading: PropTypes.string.isRequired,
  warningText: PropTypes.string.isRequired,
  handleConfirm: PropTypes.func.isRequired,
  handleClose: PropTypes.func.isRequired
};

export { WarningModal };
