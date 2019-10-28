import { createActions, handleActions } from 'redux-actions';

const ALERT_SUCCESS = 'ALERT_SUCCESS';
const ALERT_ERROR = 'ALERT_ERROR';
const CLEAR_ALERT = 'CLEAR_ALERT';

const defaultState = {};

// exports alertSuccess, alertError, clearAlert
export const alertActions = createActions(ALERT_SUCCESS, ALERT_ERROR, CLEAR_ALERT);

const { alertSuccess, alertError, clearAlert } = alertActions;
const reducer = handleActions(
  {
    [alertSuccess]: (state, { payload }) => {
      return { ...state, type: 'alert-success', message: payload };
    },
    [alertError]: (state, { payload: { message, code } }) => {
      return { ...state, type: 'alert-danger', message, code };
    },
    [clearAlert]: () => defaultState
  },
  defaultState
);

export default reducer;
