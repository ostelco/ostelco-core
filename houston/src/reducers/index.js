import { combineReducers } from 'redux';
import { authentication } from './authentication.reducer';
import { pseudonym } from './pseudo.reducer';
import { alert } from './alert.reducer';

const rootReducer = combineReducers({
  authentication,
  pseudonym,
  alert
});

export default rootReducer;
