import { createActions, handleActions } from 'redux-actions';
import { alertActions } from './alert.actions';
import { CALL_API } from '../helpers/api';
import _ from 'lodash';

const NOTIFY_REQUEST = 'NOTIFY_REQUEST';
const NOTIFY_SUCCESS = 'NOTIFY_SUCCESS';
const NOTIFY_FAILURE = 'NOTIFY_FAILURE';

const SET_NOTIFICATION_MESSAGE = 'SET_NOTIFICATION_MESSAGE';
const SET_NOTIFICATION_TITLE = 'SET_NOTIFICATION_TITLE';
const SET_NOTIFICATION_TYPE = 'SET_NOTIFICATION_TYPE';
const CLEAR_NOTIFICATION = 'CLEAR_NOTIFICATION';

// Used by global reducer.
export const notifyConstants = {
  NOTIFY_FAILURE
};

const defaultState = { type: true , message: '', title: '' };

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

const putNotificationById = (id, title, message) => ({
  [CALL_API]: {
    actions: [
      actions.notifyRequest,
      actions.notifySuccess,
      actions.notifyFailure],
    endpoint: `notify/${id}`,
    method: 'PUT',
    allowEmptyResponse: true,
    params: { message, title }
  }
});

const sendNotificationToSubscriber = (title, message) => (dispatch, getState) => {
  const handleError = (error) => {
    console.log('Error reported.', error);
    dispatch(alertActions.alertError(error));
  };

  // Get the id from the fetched user
  const subscriberId = _.get(getState(), 'customer.id');
  if (subscriberId) {
    return dispatch(putNotificationById(subscriberId, title, message))
      .catch(handleError);
  }
};

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
