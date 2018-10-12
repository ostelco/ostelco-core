import { authConstants } from '../constants';
import { authService } from '../services';

export const authActions = {
  login,
  loginSuccess,
  loginFailure,
  logout
};

function login() {
  return dispatch => {
    dispatch(request());
    authService.login()
  };
  function request() { return { type: authConstants.LOGIN_REQUEST } }
}

function loginSuccess(user) {
  return { type: authConstants.LOGIN_SUCCESS, user };
}

function loginFailure(error) {
  return { type: authConstants.LOGIN_FAILURE, error };
}

function logout() {
  authService.logout();
  return { type: authConstants.LOGOUT };
}
