import { CALL_API } from '../helpers';
import { subscriberConstants } from '../constants';
import _ from 'lodash';

const fetchSubscriberByEmail = (email) => ({
  [CALL_API]: {
    types: [
      subscriberConstants.SUBSCRIBER_BY_EMAIL_REQUEST,
      subscriberConstants.SUBSCRIBER_BY_EMAIL_SUCCESS,
      subscriberConstants.SUBSCRIBER_BY_EMAIL_FAILURE],
    endpoint: `profile/email/${encodeURIComponent(email)}`,
    method: 'GET'
  }
});

const getSubscriberByEmail = (email) => (dispatch, getState) => {
  console.log("getSubscriberByEmail =", email);
  return dispatch(fetchSubscriberByEmail(email));
}

const fetchSubscriberByMsisdn = (msisdn) => ({
  [CALL_API]: {
    types: [
      subscriberConstants.SUBSCRIBER_BY_MSISDN_REQUEST,
      subscriberConstants.SUBSCRIBER_BY_MSISDN_SUCCESS,
      subscriberConstants.SUBSCRIBER_BY_MSISDN_FAILURE],
    endpoint: `profile/msisdn/${msisdn}`,
    method: 'GET'
  }
});

const getSubscriberByMsisdn = (msisdn) => (dispatch, getState) => {
  console.log("getSubscriberByMsisdn =", msisdn);
  return dispatch(fetchSubscriberByMsisdn(msisdn));
}

const fetchBundlesByEmail = (email) => ({
  [CALL_API]: {
    types: [
      subscriberConstants.BUNDLES_REQUEST,
      subscriberConstants.BUNDLES_SUCCESS,
      subscriberConstants.BUNDLES_FAILURE],
    endpoint: `bundles/email/${encodeURIComponent(email)}`,
    method: 'GET'
  }
});

const getBundlesByEmail = (email) => (dispatch, getState) => {
  console.log("getBundlesByEmail =", email);
  return dispatch(fetchBundlesByEmail(email));
}

const fetchPaymentHistoryByEmail = (email) => ({
  [CALL_API]: {
    types: [
      subscriberConstants.PAYMENT_HISTORY_REQUEST,
      subscriberConstants.PAYMENT_HISTORY_SUCCESS,
      subscriberConstants.PAYMENT_HISTORY_FAILURE],
    endpoint: `purchases/email/${encodeURIComponent(email)}`,
    method: 'GET'
  }
});

const getPaymentHistoryByEmail = (email) => (dispatch, getState) => {
  console.log("getPaymentHistoryByEmail =", email);
  return dispatch(fetchPaymentHistoryByEmail(email));
}

// TODO: API based implementaion. Reference: https://github.com/reduxjs/redux/issues/1676
const getSubscriberAndBundlesByEmail = (email) => (dispatch, getState) =>  {
  return dispatch(fetchSubscriberByEmail(email)).then(() => {
    // Get the email from the fetched user
    const subscriberEmail = _.get(getState(), 'subscriber.email');
    if (subscriberEmail) {
      return dispatch(fetchBundlesByEmail(subscriberEmail)).then(() => {
        return dispatch(fetchPaymentHistoryByEmail(subscriberEmail))
      })
    }
  })
}


const mockGetSubscriberAndBundles = (email) => (dispatch, getState) =>  {
    // Remember I told you dispatch() can now handle thunks?
    console.log(dispatch, getState);
    dispatch(mockGetSubscriberByEmail(email));
    return dispatch(mockGetBundles(email));
}

const mockGetSubscriberByEmail = (email) => {
  const mockUser = {
    name: "Shane Warne",
    email: "shane@icc.org",
    address: "4, Leng Kee Road,  #06-07 SiS Building, Singapore 159088"
  };
  console.log("mockGetSubscriberByEmail =", email);
  return { 
    type: subscriberConstants.SUBSCRIBER_BY_EMAIL_SUCCESS,
    response: mockUser
   };
}

const mockGetBundles = (email) => {
  const mockBundle = [{
    id: "shane@icc.org",
    balance: 1024*1024*1024*3.2
  }];
  console.log("mockGetBundles =", email);
  return { 
    type: subscriberConstants.BUNDLES_SUCCESS,
    response: mockBundle
   };
}

export const subscriberActions = {
  getSubscriberByEmail,
  getSubscriberByMsisdn,
  getBundlesByEmail,
  getSubscriberAndBundlesByEmail,
  mockGetSubscriberAndBundles
};
