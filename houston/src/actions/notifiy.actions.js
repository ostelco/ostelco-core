import { createActions, handleActions } from 'redux-actions';

const NOTIFY_REQUEST = 'NOTIFY_REQUEST';
const NOTIFY_SUCCESS = 'NOTIFY_SUCCESS';
const NOTIFY_ERROR = 'NOTIFY_ERROR';

const SET_NOTIFICATION_MESSAGE = 'SET_NOTIFICATION_MESSAGE';
const SET_NOTIFICATION_TITLE = 'SET_NOTIFICATION_TITLE';
const SET_NOTIFICATION_TYPE = 'SET_NOTIFICATION_TYPE';
const CLEAR_NOTIFICATION = 'CLEAR_NOTIFICATION';

const defaultState = { type: true };

// exports alertSuccess, alertError, clearAlert
export const notifyActions = createActions(
  NOTIFY_REQUEST,
  NOTIFY_SUCCESS,
  NOTIFY_ERROR,
  SET_NOTIFICATION_MESSAGE,
  SET_NOTIFICATION_TITLE,
  SET_NOTIFICATION_TYPE,
  CLEAR_NOTIFICATION);

const { 
  notifySuccess,
  notifyError,
  clearNotification,
  setNotificationMessage,
  setNotificationTitle,
  setNotificationType,
 } = notifyActions;

 const reducer = handleActions(
  {
    [notifySuccess]: () => defaultState,
    [notifyError]: () => defaultState,
    [clearNotification]: () => defaultState,
    [setNotificationMessage]: (state, { payload }) => {
      return { ...state, message: payload };
    },
    [setNotificationTitle]: (state, { payload }) => {
      return { ...state, title: payload };
    },
    [setNotificationType]: (state, { payload }) => {
      return { ...state, type: payload };
    }
  },
  defaultState
);

export default reducer;
