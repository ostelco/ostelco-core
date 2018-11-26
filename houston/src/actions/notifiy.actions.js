import { createActions, handleActions } from 'redux-actions';
import { alertActions } from './alert.actions';
import { CALL_API } from '../helpers/api';
import _ from 'lodash';

import { encodeEmail } from '../helpers/utils';

const NOTIFY_REQUEST = 'NOTIFY_REQUEST';
const NOTIFY_SUCCESS = 'NOTIFY_SUCCESS';
const NOTIFY_FAILURE = 'NOTIFY_FAILURE';

const SET_NOTIFICATION_MESSAGE = 'SET_NOTIFICATION_MESSAGE';
const SET_NOTIFICATION_TITLE = 'SET_NOTIFICATION_TITLE';
const SET_NOTIFICATION_TYPE = 'SET_NOTIFICATION_TYPE';
const CLEAR_NOTIFICATION = 'CLEAR_NOTIFICATION';

const defaultState = { type: true };

const actions = createActions(
  NOTIFY_REQUEST,
  NOTIFY_SUCCESS,
  NOTIFY_FAILURE,
  SET_NOTIFICATION_MESSAGE,
  SET_NOTIFICATION_TITLE,
  SET_NOTIFICATION_TYPE,
  CLEAR_NOTIFICATION);

const { 
  notifySuccess,
  notifyFailure,
  clearNotification,
  setNotificationMessage,
  setNotificationTitle,
  setNotificationType,
 } = actions;

const putNotificationByEmail = (email, title, message) => ({
  [CALL_API]: {
    actions: [
      actions.notifyRequest,
      actions.notifySuccess,
      actions.notifyFailure],
    endpoint: `notify/email/${email}`,
    method: 'PUT',
    params: { message, title }
  }
});

const sendNotificationToSubscriber = (title, message) => (dispatch, getState) => {
  const handleError = (error) => {
    console.log('Error reported.', error);
    dispatch(alertActions.alertError(error));
  };
  // Get the email from the fetched user
  const subscriberEmail = encodeEmail(_.get(getState(), 'subscriber.email'));
  if (subscriberEmail) {
    return dispatch(putNotificationByEmail(subscriberEmail, title, message))
      .catch(handleError);
  }
}
export const notifyActions =  { ...actions, sendNotificationToSubscriber };

 const reducer = handleActions(
  {
    [notifySuccess]: () => defaultState,
    [notifyFailure]: () => defaultState,
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
