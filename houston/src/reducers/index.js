import { combineReducers } from 'redux';
import  _ from 'lodash';
import { authConstants, authActions } from '../actions/auth.actions';
import { store } from '../helpers';
import { subscriberConstants } from '../actions/subscriber.actions';
import { notifyConstants } from '../actions/notifiy.actions';

// Reducers.
import alert from '../actions/alert.actions';
import notification from '../actions/notifiy.actions';
import authentication from './auth.reducer';
import { context, subscriber, subscriptions, bundles, paymentHistory } from './subscriber.reducer';


const appReducer = combineReducers({
  authentication,
  alert,
  notification,
  subscriber,
  context,
  subscriptions,
  bundles,
  paymentHistory
});

function checkForAuthenticationFailures(errorObj) {
  if (errorObj && errorObj.code === authConstants.AUTHENTICATION_FAILURE) {
    setTimeout(() => {
      store.dispatch(authActions.logout());
    });
  }
}

const rootReducer = (state, action) => {
  if (action.type === authConstants.LOGOUT) {
    state = {};
  }
  switch (action.type) {
    case authConstants.LOGIN_FAILURE:
    case subscriberConstants.SUBSCRIPTIONS_FAILURE:
    case subscriberConstants.SUBSCRIBER_BY_EMAIL_FAILURE:
    case notifyConstants.NOTIFY_FAILURE:
      checkForAuthenticationFailures(_.get(action, 'payload.errorObj'));
      break;
    default:
      break;
  }
  return appReducer(state, action);
};

export default rootReducer;
