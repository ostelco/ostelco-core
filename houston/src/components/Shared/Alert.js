import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import { Alert } from 'reactstrap';

import { alertActions } from '../../actions';
import { subscriberActions } from '../../actions';

function AlertMessage(props) {
  function onDismiss(e) {
    props.clear();
  }

  const visible = (props.alert && props.alert.type === 'alert-danger');
  if (!visible) return null;
  return (
    <Alert color="danger" isOpen={visible} toggle={onDismiss}>
      {props.alert.message}
      <hr />
      {props.alert.error}
    </Alert>
  );
}

AlertMessage.propTypes = {
  clear: PropTypes.func.isRequired,
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
  clear: alertActions.clear,
  getBundlesByEmail: subscriberActions.getBundlesByEmail
}
export default connect(mapStateToProps, mapDispatchToProps)(AlertMessage);
