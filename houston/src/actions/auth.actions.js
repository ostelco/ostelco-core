import { createActions } from 'redux-actions';

const LOGIN_REQUEST = 'LOGIN_REQUEST';
const LOGIN_SUCCESS = 'LOGIN_SUCCESS';
const LOGIN_FAILURE = 'LOGIN_FAILURE';

const LOGOUT = 'LOGOUT';
const AUTHENTICATION_FAILURE = 'AUTHENTICATION_FAILURE';

export const authActions = createActions(
  LOGIN_REQUEST,
  LOGIN_SUCCESS,
  LOGIN_FAILURE,
  LOGOUT
);
