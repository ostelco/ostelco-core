import { handleActions } from 'redux-actions';

import { authService } from '../services';
import { authActions } from '../actions';

let user = JSON.parse(localStorage.getItem('user'));
const defaultState = user ? { loggedIn: true, user } : {};

const { loginRequest, loginSuccess, loginFailure, logout } = authActions;

const reducer = handleActions(
  {
    [loginRequest]: () => {
      authService.login()
      return { loggingIn: true };
    },
    [loginSuccess]: (state, { payload }) => {
      return { loggedIn: true, user: payload };
    },
    [logout]: () => {
      authService.logout();
      return {};
    },
    [loginFailure]: () => {
      authService.logout();
      return {};
    }
  },
  defaultState
);

export default reducer;
