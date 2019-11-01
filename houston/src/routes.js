import React from 'react';
import { Route, BrowserRouter as Router, Redirect, Switch, Link } from 'react-router-dom';
import { connect } from 'react-redux';

import App from './components/App';
import Main from './components/Search/Main';
import Notifications from './components/Notifications/Notifications';
import Callback from './components/Callback/Callback';
import { Provider } from 'react-redux';
import { store } from './helpers';
import { authService } from './services';

const handleAuthentication = ({ location }) => {
  if (/access_token|id_token|error/.test(location.hash)) {
    authService.handleAuthentication(store.dispatch, location.hash);
  }
};

function ProtectedRoute({ component: Component, ...rest }) {
  return (
    <Route
      {...rest}
      render={(props) => {
        return authService.isAuthenticated() ? (
          <Component {...props} />
        ) : (
            <Redirect
              to={{
                pathname: "/login",
                state: { from: props.location }
              }}
            />
          );
      }
      }
    />
  );
}

function login(props) {
  // Redirect to search 
  if (props.loggedIn) {
    return <Redirect to="/" />;
  }
  return (
    <div className="container">
      <h4>You are not logged in! Please Log In to continue.</h4>
    </div>
  );
}

function mapStateToProps(state) {
  const { loggedIn } = state.authentication;
  return {
    loggedIn
  };
};
const Login = connect(mapStateToProps)(login);

function NoMatch() {
  return (
    <div className="container">
      <p>No such path found here...</p>
      <ul>
        <li>
          <Link to="/">Search</Link>
        </li>
        <li>
          <Link to="/notifications">Notifications</Link>
        </li>
      </ul>
    </div>
  );
}

export const makeMainRoutes = () => {
  return (
    <Provider store={store}>
      <Router>
        <div>
          <App />
          <Switch >
            <Route path="/login" component={Login} />
            <ProtectedRoute path="/" exact component={Main} />
            <ProtectedRoute path="/notifications" component={Notifications} />
            <Route path="/callback" render={(props) => {
              handleAuthentication(props);
              return <Callback {...props} />
            }} />
            <Route component={NoMatch} />
          </Switch>
        </div>
      </Router>
    </Provider>
  );
}

