import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import { Alert } from 'reactstrap';

import { alertActions } from '../../actions/alert.actions';
import { authConstants } from '../../actions/auth.actions';

function AlertMessage({ alert, clearAlert }) {

  function onDismiss(e) {
    clearAlert();
  }

  const { code, message, type } = alert;
  console.log(`Alert Type = ${type}`);
  if (type !== 'error' || code === authConstants.AUTHENTICATION_FAILURE) {
    // Don't show Authentication failed message
    return null;
  }

  return (
    <Alert color="danger" toggle={onDismiss}>
      {message}
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
