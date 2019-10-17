import _ from 'lodash';
import { createActions } from 'redux-actions';

import { CALL_API } from '../helpers/api';
import { alertActions } from './alert.actions';
import { customerActions } from './cutomer.actions';
import { encodeEmail } from '../helpers/utils';

const SUBSCRIBER_BY_EMAIL_REQUEST = 'SUBSCRIBER_BY_EMAIL_REQUEST';
const SUBSCRIBER_BY_EMAIL_SUCCESS = 'SUBSCRIBER_BY_EMAIL_SUCCESS';
const SUBSCRIBER_BY_EMAIL_FAILURE = 'SUBSCRIBER_BY_EMAIL_FAILURE';

const CONTEXT_BY_EMAIL_REQUEST = 'CONTEXT_BY_EMAIL_REQUEST';
const CONTEXT_BY_EMAIL_SUCCESS = 'CONTEXT_BY_EMAIL_SUCCESS';
const CONTEXT_BY_EMAIL_FAILURE = 'CONTEXT_BY_EMAIL_FAILURE';

const SUBSCRIPTIONS_REQUEST = 'SUBSCRIPTIONS_REQUEST';
const SUBSCRIPTIONS_SUCCESS = 'SUBSCRIPTIONS_SUCCESS';
const SUBSCRIPTIONS_FAILURE = 'SUBSCRIPTIONS_FAILURE';

const BUNDLES_REQUEST = 'BUNDLES_REQUEST';
const BUNDLES_SUCCESS = 'BUNDLES_SUCCESS';
const BUNDLES_FAILURE = 'BUNDLES_FAILURE';

const PAYMENT_HISTORY_REQUEST = 'PAYMENT_HISTORY_REQUEST';
const PAYMENT_HISTORY_SUCCESS = 'PAYMENT_HISTORY_SUCCESS';
const PAYMENT_HISTORY_FAILURE = 'PAYMENT_HISTORY_FAILURE';

const REFUND_PAYMENT_REQUEST = 'REFUND_PAYMENT_REQUEST';
const REFUND_PAYMENT_SUCCESS = 'REFUND_PAYMENT_SUCCESS';
const REFUND_PAYMENT_FAILURE = 'REFUND_PAYMENT_FAILURE';

const AUDIT_LOGS_REQUEST = 'AUDIT_LOGS_REQUEST';
const AUDIT_LOGS_SUCCESS = 'AUDIT_LOGS_SUCCESS';
const AUDIT_LOGS_FAILURE = 'AUDIT_LOGS_FAILURE';

const DELETE_USER_REQUEST = 'DELETE_USER_REQUEST';
const DELETE_USER_SUCCESS = 'DELETE_USER_SUCCESS';
const DELETE_USER_FAILURE = 'DELETE_USER_FAILURE';

// Used by global reducer.
export const subscriberConstants = {
  SUBSCRIBER_BY_EMAIL_FAILURE,
  SUBSCRIPTIONS_FAILURE,
  DELETE_USER_SUCCESS,
  DELETE_USER_FAILURE,
};

export const actions = createActions(
  SUBSCRIBER_BY_EMAIL_REQUEST,
  SUBSCRIBER_BY_EMAIL_SUCCESS,
  SUBSCRIBER_BY_EMAIL_FAILURE,
  CONTEXT_BY_EMAIL_REQUEST,
  CONTEXT_BY_EMAIL_SUCCESS,
  CONTEXT_BY_EMAIL_FAILURE,
  SUBSCRIPTIONS_REQUEST,
  SUBSCRIPTIONS_SUCCESS,
  SUBSCRIPTIONS_FAILURE,
  BUNDLES_REQUEST,
  BUNDLES_SUCCESS,
  BUNDLES_FAILURE,
  PAYMENT_HISTORY_REQUEST,
  PAYMENT_HISTORY_SUCCESS,
  PAYMENT_HISTORY_FAILURE,
  REFUND_PAYMENT_REQUEST,
  REFUND_PAYMENT_SUCCESS,
  REFUND_PAYMENT_FAILURE,
  AUDIT_LOGS_REQUEST,
  AUDIT_LOGS_SUCCESS,
  AUDIT_LOGS_FAILURE,
  DELETE_USER_REQUEST,
  DELETE_USER_SUCCESS,
  DELETE_USER_FAILURE
);

const fetchSubscriberById = (id) => ({
  [CALL_API]: {
    actions: [
      actions.subscriberByEmailRequest,
      actions.subscriberByEmailSuccess,
      actions.subscriberByEmailFailure],
    endpoint: `profiles/${id}`,
    method: 'GET'
  }
});

const fetchContextById = (id) => ({
  [CALL_API]: {
    actions: [
      actions.contextByEmailRequest,
      actions.contextByEmailSuccess,
      actions.contextByEmailFailure],
    endpoint: `context/${id}`,
    method: 'GET'
  }
});

const fetchSubscriptionsById = (id) => ({
  [CALL_API]: {
    actions: [
      actions.subscriptionsRequest,
      actions.subscriptionsSuccess,
      actions.subscriptionsFailure],
    endpoint: `profiles/${id}/subscriptions`,
    method: 'GET'
  }
});

const fetchBundlesById = (id) => ({
  [CALL_API]: {
    actions: [
      actions.bundlesRequest,
      actions.bundlesSuccess,
      actions.bundlesFailure],
    endpoint: `bundles/${id}`,
    method: 'GET'
  }
});

const fetchPaymentHistoryById = (id) => ({
  [CALL_API]: {
    actions: [
      actions.paymentHistoryRequest,
      actions.paymentHistorySuccess,
      actions.paymentHistoryFailure],
    endpoint: `purchases/${id}`,
    method: 'GET'
  }
});

const putRefundPurchaseById = (id, purchaseRecordId, reason) => ({
  [CALL_API]: {
    actions: [
      actions.refundPaymentRequest,
      actions.refundPaymentSuccess,
      actions.refundPaymentFailure],
    endpoint: `refund/${id}`,
    method: 'PUT',
    params: { purchaseRecordId, reason }
  }
});

const fetchAuditLogsById = (id) => ({
  [CALL_API]: {
    actions: [
      actions.auditLogsRequest,
      actions.auditLogsSuccess,
      actions.auditLogsFailure],
    endpoint: `auditLog/${id}`,
    method: 'GET'
  }
});

const deleteUserById = (id) => ({
  [CALL_API]: {
    actions: [
      actions.deleteUserRequest,
      actions.deleteUserSuccess,
      actions.deleteUserFailure],
    endpoint: `customer/${id}`,
    allowEmptyResponse: true,
    method: 'DELETE'
  }
});

// TODO: API based implementaion. Reference: https://github.com/reduxjs/redux/issues/1676
const getSubscriberAndBundlesByEmail = (email) => (dispatch, getState) => {
  dispatch(customerActions.clearCustomer());
  localStorage.setItem('searchedEmail', email)

  email = encodeEmail(email);
  const handleError = (error) => {
    console.log('Error reported.', error);
    dispatch(alertActions.alertError(error));
  };

  return dispatch(fetchSubscriberById(email))
    .then(() => {
      // Get the id from the fetched user
      const subscriber = _.get(getState(), 'subscriber[0]');
      const subscriberId = _.get(getState(), 'subscriber[0].id');
      if (subscriberId) {
        dispatch(customerActions.selectCustomer(subscriber));
        dispatch(fetchContextById(subscriberId)).catch(handleError);
        dispatch(fetchAuditLogsById(subscriberId)).catch(handleError);
        dispatch(fetchSubscriptionsById(subscriberId)).catch(handleError);
        return dispatch(fetchBundlesById(subscriberId))
          .then(() => {
            return dispatch(fetchPaymentHistoryById(subscriberId));
          })
          .catch(handleError);
      }
    })
    .catch(handleError);
};

const refundPurchase = (purchaseRecordId, reason) => (dispatch, getState) => {

  const handleError = (error) => {
    console.log('Error reported.', error);
    dispatch(alertActions.alertError(error));
  };

  // Get the id from the fetched user
  const subscriberId = _.get(getState(), 'subscriber[0].id');
  if (subscriberId) {
    return dispatch(putRefundPurchaseById(subscriberId, purchaseRecordId, reason))
      .then(() => {
        return dispatch(fetchPaymentHistoryById(subscriberId));
      })
      .catch(handleError);
  }
};
const deleteUser = () => (dispatch, getState) => {

  const handleError = (error) => {
    console.log('Error reported.', error.message);
    let message = "Failed to delete user (" +error.message+")"
    dispatch(alertActions.alertError({message}));
  };

  // Get the id from the fetched user
  const subscriberId = _.get(getState(), 'subscriber[0].id');
  if (subscriberId) {
    return dispatch(deleteUserById(subscriberId))
      .catch(handleError);
  }
};
export const subscriberActions = {
  getSubscriberAndBundlesByEmail,
  refundPurchase,
  deleteUser
};
