import { createActions, handleActions } from 'redux-actions';

const SELECT_SUBSCRIBER = 'SELECT_SUBSCRIBER';
const CLEAR_SUBSCRIBER = 'CLEAR_SUBSCRIBER';

const defaultState = {};

const actions = createActions(
  SELECT_SUBSCRIBER,
  CLEAR_SUBSCRIBER);

const { 
  selectSubscriber,
  clearSubscriber,
} = actions;


export const currentSubscriberActions =  { ...actions };

 const reducer = handleActions(
  {
    [selectSubscriber]: (state, { payload }) => {
      return { ...payload };
    },
    [clearSubscriber]: () => {
        return defaultState;
      },
    },
  defaultState
);

export default reducer;
