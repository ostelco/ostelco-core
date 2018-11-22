import { createStore, applyMiddleware, compose } from 'redux';
import thunk from 'redux-thunk';
import { createLogger } from 'redux-logger';
import rootReducer from '../reducers';
import api from './api';
import { composeWithDevTools } from 'redux-devtools-extension';
import { isChrome } from './utils';

const logger = createLogger();

let composeEnhancers = composeWithDevTools({
  // Specify name here, actionsBlacklist, actionsCreators and other options if needed
});
if (!isChrome()) {
  composeEnhancers = compose;
}
export const store = createStore(rootReducer, /* preloadedState, */ composeEnhancers(
  applyMiddleware(thunk, api, logger)
));
