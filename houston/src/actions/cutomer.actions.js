import { createActions, handleActions } from 'redux-actions';

const SELECT_CUSTOMER = 'SELECT_CUSTOMER';
const CLEAR_CUSTOMER = 'CLEAR_CUSTOMER';

const defaultState = {};

const actions = createActions(
  SELECT_CUSTOMER,
  CLEAR_CUSTOMER);

const { 
  selectCustomer,
  clearCustomer,
} = actions;


export const customerActions =  { ...actions };

 const reducer = handleActions(
  {
    [selectCustomer]: (state, { payload }) => {
      return { ...payload };
    },
    [clearCustomer]: () => {
        return defaultState;
      },
    },
  defaultState
);

export default reducer;
