import { history } from '../helpers';
import auth0 from 'auth0-js';
import * as _ from 'lodash';
import { getAuthConfig } from './config-variables';
import { authActions } from '../actions';
import { store } from '../helpers';

const authConfig  = getAuthConfig();
class Auth {
  auth0 = new auth0.WebAuth({
    domain: authConfig.domain,
    clientID: authConfig.clientId,
    redirectUri: authConfig.callbackUrl,
    responseType: 'token id_token',
    scope: 'openid profile email',
    audience: 'http://google_api'
  });

  constructor() {
    this.user = {};
    this.login = this.login.bind(this);
    this.logout = this.logout.bind(this);
    this.handleAuthentication = this.handleAuthentication.bind(this);
    this.loadCurrentSession = this.loadCurrentSession.bind(this);
    this.isAuthenticated = this.isAuthenticated.bind(this);
    console.log("constructing Auth");
    setTimeout(this.loadCurrentSession, 1);
  }

  login() {
    this.auth0.authorize();
  }

  handleAuthentication(dispatch) {
    this.auth0.parseHash((err, authResult) => {
      if (authResult && authResult.accessToken) {
        this.setSession(authResult);
        dispatch(authActions.loginSuccess(this.user));
      } else if (err) {
        console.log(err);
        alert(`Error: ${err.error}. Check the console for further details.`);
        this.logout()
        dispatch(authActions.loginFailure(err));
      }
    });
  }

  setSession(authResult) {
    console.log(authResult);
    // Set the time that the access token will expire at
    let expiresAt = JSON.stringify((authResult.expiresIn * 1000) + new Date().getTime());
    localStorage.setItem('access_token', authResult.accessToken);
    localStorage.setItem('expires_at', expiresAt);
    const name = _.get(authResult, 'idTokenPayload.name');
    localStorage.setItem('name', name);
    const email = _.get(authResult, 'idTokenPayload.email')
    localStorage.setItem('email', email);
    const picture = _.get(authResult, 'idTokenPayload.picture')
    localStorage.setItem('picture', picture);

    // navigate to the home route
    history.replace('/home');
    const { accessToken } = authResult;
    this.user = { accessToken, expiresAt, name, email, picture };
  }

  loadCurrentSession() {
    console.log("loadCurrentSession");
    const accessToken = localStorage.getItem('access_token');
    const expiresAt = localStorage.getItem('expires_at');
    const name = localStorage.getItem('name');
    const email = localStorage.getItem('email');
    const picture = localStorage.getItem('picture');
    const isAuthenticated = this.isAuthenticated(expiresAt);
    this.user = { accessToken, expiresAt, name, email, picture };
    if (isAuthenticated) {
      history.replace('/home');
      store.dispatch(authActions.loginSuccess(this.user));
    } else {
      store.dispatch(authActions.logout())
    }
  }

  logout() {
    this.user = {};
    // Clear access token and ID token from local storage
    localStorage.removeItem('access_token');
    localStorage.removeItem('expires_at');
    localStorage.removeItem('name');
    localStorage.removeItem('email');
    localStorage.removeItem('picture');
    // navigate to the home route
    history.replace('/home');
  }

  isAuthenticated(expiresAt) {
    // Check whether the current time is past the
    // access token's expiry time
    let expiry = JSON.parse(expiresAt);
    return new Date().getTime() < expiry;
  }

  authHeader() {
    const state = store.getState();
    const user = _.get(state, "authentication.user");
    if (user && this.isAuthenticated(user.expiresAt)) {
      return `Bearer ${user.accessToken}`;
    }
    return null;
  }
}
const auth = new Auth();
export const authService = auth;
