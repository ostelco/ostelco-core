import { combineReducers } from 'redux';
import { authentication } from './authentication.reducer';
import { subscriber, bundles, paymentHistory } from './subscriber.reducer';
import { authConstants, subscriberConstants } from '../constants';
import { authActions } from '../actions';
import alert from '../actions/alert.actions';
import { store } from '../helpers';

const appReducer = combineReducers({
  authentication,
  alert,
  subscriber,
  bundles,
  paymentHistory
});

function checkForAuthenticationFailures(errorObj) {
  if (errorObj && errorObj.code === authConstants.AUTHENTICATION_FAILURE) {
    setTimeout(() => {
      store.dispatch(authActions.logout());
    }, 1);
  }
}

const rootReducer = (state, action) => {
  if (action.type === authConstants.LOGOUT) {
    state = {}
  }
  switch (action.type) {
    case authConstants.LOGIN_FAILURE:
    case subscriberConstants.SUBSCRIBER_BY_MSISDN_FAILURE:
    case subscriberConstants.SUBSCRIBER_BY_EMAIL_FAILURE:
      checkForAuthenticationFailures(action.errorObj);
      break;
  }
  return appReducer(state, action);
};
export default rootReducer;
