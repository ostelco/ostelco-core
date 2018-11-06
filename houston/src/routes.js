import React from 'react';
import { Route, Router } from 'react-router-dom';
import App from './components/App';
import Home from './components/Home/Home';
import Search from './components/Search/Search';
import Callback from './components/Callback/Callback';
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
  console.log("makeMainRoutes")
  return (
    <Provider store={store}>
        <Router history={history}>
            <div>
            <Route path="/" render={(props) => <App {...props} />} />
            <Route path="/home" render={(props) => <Home {...props} />} />
            <Route path="/search" render={(props) => <Search {...props} />} />
            <Route path="/callback" render={(props) => {
                handleAuthentication(props);
                return <Callback {...props} /> 
            }}/>
            </div>
        </Router>
      </Provider>
  );
}
