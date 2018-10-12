import React from 'react';
import { Route, Router } from 'react-router-dom';
import App from './App';
import Home from './Home/Home';
import Callback from './Callback/Callback';
import { history } from './helpers';
import { Provider } from 'react-redux';
import { store } from './helpers';
import { authService } from './services';

const handleAuthentication = ({location}) => {
  if (/access_token|id_token|error/.test(location.hash)) {
      authService.handleAuthentication(store.dispatch);
  }
}

export const makeMainRoutes = () => {
  return (
    <Provider store={store}>
        <Router history={history}>
            <div>
            <Route path="/" render={(props) => <App {...props} />} />
            <Route path="/home" render={(props) => <Home {...props} />} />
            <Route path="/callback" render={(props) => {
                handleAuthentication(props);
                return <Callback {...props} /> 
            }}/>
            </div>
        </Router>
      </Provider>
  );
}
