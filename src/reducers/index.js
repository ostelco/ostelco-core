import { combineReducers } from 'redux';

import { authentication } from './authentication.reducer';
import { users } from './users.reducer';
import { pseudonym } from './pseudo.reducer';
import { alert } from './alert.reducer';

const rootReducer = combineReducers({
  authentication,
  pseudonym,
  users,
  alert
});

export default rootReducer;