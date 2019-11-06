import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import { Alert } from 'reactstrap';

import { alertActions } from '../../actions/alert.actions';
import { authConstants } from '../../actions/auth.actions';

function AlertMessage({ alert, clearAlert }) {
  const { code, message, type } = alert;
  const isOpen = (type === 'error' && code !== authConstants.AUTHENTICATION_FAILURE)

  return (
    <Alert color="danger" isOpen={isOpen} toggle={() => clearAlert()}>
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
