import React from 'react';
import { Route, Router } from 'react-router-dom';
import App from './components/App';
import Search from './components/Search/Search';
import Notifications from './components/Notifications/Notifications';
import Callback from './components/Callback/Callback';
import { history } from './helpers';
import { Provider } from 'react-redux';
import { store } from './helpers';
import { authService } from './services';

const handleAuthentication = ({ location }) => {
  if (/access_token|id_token|error/.test(location.hash)) {
    authService.handleAuthentication(store.dispatch, location.hash);
  }
}

export const makeMainRoutes = () => {
  return (
    <Provider store={store}>
      <Router history={history}>
        <div>
          <Route path="/" render={(props) => <App {...props} />} />
          <Route path="/search" render={(props) => <Search {...props} />} />
          <Route path="/notifications" render={(props) => <Notifications {...props} />} />
          <Route path="/callback" render={(props) => {
            handleAuthentication(props);
            return <Callback {...props} />
          }} />
        </div>
      </Router>
    </Provider>
  );
}
