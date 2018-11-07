import { CALL_API } from '../helpers';
import { subscriberConstants } from '../constants';

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

// TODO: API based implementaion. Reference: https://github.com/reduxjs/redux/issues/1676

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
  mockGetSubscriberAndBundles
};
