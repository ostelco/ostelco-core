import { handleActions } from 'redux-actions'
import { actions } from '../actions/subscriber.actions';

const defaultState = {};

export const subscribers = handleActions(
  {
    [actions.subscriberByEmailRequest]: (state, action) => ({
      loading: true
    }),
    [actions.subscriberByEmailSuccess]: (state, action) => (action.payload), // Array of subscribers
    [actions.subscriberByEmailFailure]: (state, action) => ({
      ...action.payload
    })
  },
  defaultState
);

export const context = handleActions(
  {
    [actions.contextByEmailRequest]: (state, action) => ({
      loading: true
    }),
    [actions.contextByEmailSuccess]: (state, action) => ({
      ...action.payload
    }),
    [actions.contextByEmailFailure]: (state, action) => ({
      ...action.payload
    })
  },
  defaultState
);

export const subscriptions = handleActions(
  {
    [actions.subscriptionsRequest]: (state, action) => ({
      loading: true
    }),
    [actions.subscriptionsSuccess]: (state, action) => ({
      items: action.payload
    }),
    [actions.subscriptionsFailure]: (state, action) => ({
      ...action.payload
    })
  },
  defaultState
);

export const bundles = handleActions(
  {
    [actions.bundlesRequest]: (state, action) => ({
      loading: true
    }),
    [actions.bundlesSuccess]: (state, action) => action.payload,
    [actions.bundlesFailure]: (state, action) => ({
      ...action.payload
    })
  },
  defaultState
);

export const paymentHistory = handleActions(
  {
    [actions.paymentHistoryRequest]: (state, action) => ({
      loading: true
    }),
    [actions.paymentHistorySuccess]: (state, action) => action.payload,
    [actions.paymentHistoryFailure]: (state, action) => ({
      ...action.payload
    })
  },
  defaultState
);

export const auditLogs = handleActions(
  {
    [actions.auditLogsRequest]: (state, action) => ({
      loading: true
    }),
    [actions.auditLogsSuccess]: (state, action) => action.payload,
    [actions.auditLogsFailure]: (state, action) => ({
      ...action.payload
    })
  },
  []
);
