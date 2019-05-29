import _ from 'lodash';
import { createActions } from 'redux-actions';

import { CALL_API } from '../helpers/api';
import { alertActions } from './alert.actions';
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

// Used by global reducer.
export const subscriberConstants = {
  SUBSCRIBER_BY_EMAIL_FAILURE,
  SUBSCRIPTIONS_FAILURE,
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
  REFUND_PAYMENT_FAILURE
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

const fetchContextByEmail = (email) => ({
  [CALL_API]: {
    actions: [
      actions.contextByEmailRequest,
      actions.contextByEmailSuccess,
      actions.contextByEmailFailure],
    endpoint: `context/${email}`,
    method: 'GET'
  }
});

const fetchSubscriptionsByEmail = (email) => ({
  [CALL_API]: {
    actions: [
      actions.subscriptionsRequest,
      actions.subscriptionsSuccess,
      actions.subscriptionsFailure],
    endpoint: `profiles/${email}/subscriptions`,
    method: 'GET'
  }
});

const fetchBundlesByEmail = (email) => ({
  [CALL_API]: {
    actions: [
      actions.bundlesRequest,
      actions.bundlesSuccess,
      actions.bundlesFailure],
    endpoint: `bundles/${email}`,
    method: 'GET'
  }
});

const fetchPaymentHistoryByEmail = (email) => ({
  [CALL_API]: {
    actions: [
      actions.paymentHistoryRequest,
      actions.paymentHistorySuccess,
      actions.paymentHistoryFailure],
    endpoint: `purchases/${email}`,
    method: 'GET'
  }
});

const putRefundPurchaseByEmail = (email, purchaseRecordId, reason) => ({
  [CALL_API]: {
    actions: [
      actions.refundPaymentRequest,
      actions.refundPaymentSuccess,
      actions.refundPaymentFailure],
    endpoint: `refund/${email}`,
    method: 'PUT',
    params: { purchaseRecordId, reason }
  }
});

// TODO: API based implementaion. Reference: https://github.com/reduxjs/redux/issues/1676
const getSubscriberAndBundlesByEmail = (email) => (dispatch, getState) => {
  email = encodeEmail(email);
  const handleError = (error) => {
    console.log('Error reported.', error);
    dispatch(alertActions.alertError(error));
  };

  return dispatch(fetchSubscriberById(email))
    .then(() => {
      // Get the email from the fetched user
      const subscriberEmail = encodeEmail(_.get(getState(), 'subscriber.contactEmail'));
      if (subscriberEmail) {
        dispatch(fetchContextByEmail(subscriberEmail)).catch(handleError);
        dispatch(fetchSubscriptionsByEmail(subscriberEmail)).catch(handleError);
        return dispatch(fetchBundlesByEmail(subscriberEmail))
          .then(() => {
            return dispatch(fetchPaymentHistoryByEmail(subscriberEmail));
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

  // Get the email from the fetched user
  const subscriberEmail = encodeEmail(_.get(getState(), 'subscriber.contactEmail'));
  if (subscriberEmail) {
    return dispatch(putRefundPurchaseByEmail(subscriberEmail, purchaseRecordId, reason))
      .then(() => {
        return dispatch(fetchPaymentHistoryByEmail(subscriberEmail));
      })
      .catch(handleError);
  }
};

export const subscriberActions = {
  getSubscriberAndBundlesByEmail,
  refundPurchase
};
