import { subscriberConstants } from '../constants';

export function subscriber(state = {}, action) {
  switch (action.type) {
    case subscriberConstants.SUBSCRIBER_BY_EMAIL_REQUEST:
    case subscriberConstants.SUBSCRIBER_BY_MSISDN_REQUEST:
      return {
        loading: true
      };
    case subscriberConstants.SUBSCRIBER_BY_EMAIL_SUCCESS:
    case subscriberConstants.SUBSCRIBER_BY_MSISDN_SUCCESS:
      return action.response
    case subscriberConstants.SUBSCRIBER_BY_EMAIL_FAILURE:
    case subscriberConstants.SUBSCRIBER_BY_MSISDN_FAILURE:
      return {
        error: action.error
      };
    default:
      return state
  }
}

export function bundles(state = {}, action) {
  switch (action.type) {
    case subscriberConstants.BUNDLES_REQUEST:
      return {
        loading: true
      };
    case subscriberConstants.BUNDLES_SUCCESS:
      return {
        data: action.response
      };
    case subscriberConstants.BUNDLES_FAILURE:
      return {
        error: action.error
      };
    default:
      return state
  }
}

export function paymentHistory(state = {}, action) {
  switch (action.type) {
    case subscriberConstants.PAYMENT_HISTORY_REQUEST:
      return {
        loading: true
      };
    case subscriberConstants.PAYMENT_HISTORY_SUCCESS:
      let history = action.response;
      if (Array.isArray(history)){
        history.sort((a,b) => {
          return (a.timestamp - b.timestamp) * -1;
        })
      }
      return {
        data: history
      };
    case subscriberConstants.PAYMENT_HISTORY_FAILURE:
      return {
        error: action.error
      };
    default:
      return state
  }
}
