import { createActions } from 'redux-actions';

export const authConstants = {
  LOGIN_REQUEST: 'LOGIN_REQUEST',
  LOGIN_SUCCESS: 'LOGIN_SUCCESS',
  LOGIN_FAILURE: 'LOGIN_FAILURE',

  LOGOUT: 'LOGOUT',
  AUTHENTICATION_FAILURE: 'AUTHENTICATION_FAILURE',
};

export const authActions = createActions(
  authConstants.LOGIN_REQUEST,
  authConstants.LOGIN_SUCCESS,
  authConstants.LOGIN_FAILURE,
  authConstants.LOGOUT
);
