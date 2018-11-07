import { combineReducers } from 'redux';
import { authentication } from './authentication.reducer';
import { pseudonym } from './pseudo.reducer';
import { alert } from './alert.reducer';
import { subscriber, bundles } from './subscriber.reducer';

const rootReducer = combineReducers({
  authentication,
  pseudonym,
  alert,
  subscriber,
  bundles
});

export default rootReducer;
