import { userConstants } from '../constants';
import { userService } from '../services';

export const userActions = {
  login,
  loginSuccess,
  loginFailure,
  logout,
  getAll
};

function login() {
  console.log(userService);
  return dispatch => {
    dispatch(request());
    userService.login()
  };
  function request() { return { type: userConstants.LOGIN_REQUEST } }
}

function loginSuccess(user) {
  return { type: userConstants.LOGIN_SUCCESS, user };
}

function loginFailure(error) {
  return { type: userConstants.LOGIN_FAILURE, error };
}

function logout() {
  userService.logout();
  return { type: userConstants.LOGOUT };
}

function getAll() {
  return dispatch => {
    dispatch(request());

    userService.getAll()
      .then(
        users => dispatch(success(users)),
        error => dispatch(failure(error))
      );
  };

  function request() { return { type: userConstants.GETALL_REQUEST } }
  function success(users) { return { type: userConstants.GETALL_SUCCESS, users } }
  function failure(error) { return { type: userConstants.GETALL_FAILURE, error } }
}