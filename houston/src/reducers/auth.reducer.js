import { handleActions } from 'redux-actions';

import { authService } from '../services';
import { authActions } from '../actions/auth.actions';

let user = JSON.parse(localStorage.getItem('user'));
const defaultState = user ? { loggedIn: true, user } : {};

const reducer = handleActions(
  {
    [authActions.loginRequest]: () => {
      authService.login();
      return { loggingIn: true };
    },
    [authActions.loginSuccess]: (state, { payload }) => {
      return { loggedIn: true, user: payload };
    },
    [authActions.logout]: () => {
      authService.logout();
      return {};
    },
    [authActions.loginFailure]: () => {
      authService.logout();
      return {};
    }
  },
  defaultState
);

export default reducer;
