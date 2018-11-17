import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import { Alert } from 'reactstrap';

import { alertActions } from '../../actions';

function AlertMessage(props) {
  function onDismiss(e) {
    props.clearAlert();
  }

  const visible = (props.alert && props.alert.type === 'alert-danger');
  if (!visible) return null;
  return (
    <Alert color="danger" isOpen={visible} toggle={onDismiss}>
      {props.alert.message}
      <hr />
    </Alert>
  );
}

AlertMessage.propTypes = {
  clearAlert: PropTypes.func.isRequired,
  alert: PropTypes.shape({
    type: PropTypes.string,
    message: PropTypes.string
  })
};

function mapStateToProps(state) {
  const { alert } = state;
  return {
    alert
  };
}
const mapDispatchToProps = {
  clearAlert: alertActions.clearAlert,
}
export default connect(mapStateToProps, mapDispatchToProps)(AlertMessage);
