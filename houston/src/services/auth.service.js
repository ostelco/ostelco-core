import auth0 from 'auth0-js';
import _ from 'lodash';

import { getAuthConfig } from './config-variables';
import { authConstants, authActions } from '../actions/auth.actions';
import { store } from '../helpers';
import { setAuthResolver } from '../helpers/api';

const authConfig = getAuthConfig();
class Auth {
  webAuth = new auth0.WebAuth({
    domain: authConfig.domain,
    clientID: authConfig.clientId,
    redirectUri: authConfig.callbackUrl,
    responseType: 'token id_token',
    scope: 'openid profile email',
    audience: 'http://google_api'
  });

  constructor() {
    this.user = null;
    setAuthResolver(this.getHeader);
    this.loadCurrentSession();
  }

  login() {
    this.webAuth.authorize();
  }

  handleAuthentication(dispatch, hash) {
    if (this.user != null) {
      console.log('Valid session in memory, callback is called before the session is cleared.');
      return;
    }
    this.user = {}; // initialize
    this.webAuth.parseHash({ hash }, (err, authResult) => {
      if (authResult && authResult.accessToken) {
        this.setSession(authResult);
        // navigate to the home route
        dispatch(authActions.loginSuccess(this.user));
      } else if (err) {
        console.log('Error recieved from auth0 parse', err);
        this.clearLocalStorage();
        if (err.error === 'invalid_token') {
          // Token expired, re-login
          console.log('Invalid token recieved, possibly expired token');
        }
        dispatch(authActions.loginFailure(err));
      }
    });
  }

  setSession(authResult) {
    // Set the time that the access token will expire at
    let expiresAt = JSON.stringify((authResult.expiresIn * 1000) + new Date().getTime());
    const { accessToken } = authResult;
    localStorage.setItem('access_token', accessToken);
    localStorage.setItem('expires_at', expiresAt);

    const name = _.get(authResult, 'idTokenPayload.name');
    const email = _.get(authResult, 'idTokenPayload.email');
    const picture = _.get(authResult, 'idTokenPayload.picture');
    localStorage.setItem('name', name);
    localStorage.setItem('email', email);
    localStorage.setItem('picture', picture);

    this.user = { accessToken, expiresAt, name, email, picture };
  }

  loadCurrentSession = () => {
    if (this.user !== null) {
      console.log('Valid session in memory, clear this before calling loadCurrentSession');
      return;
    }
    const accessToken = localStorage.getItem('access_token');
    if (accessToken === null) {
      console.log('No saved user');
      return;
    }
    const expiresAt = localStorage.getItem('expires_at');
    if (!this.isTokenValid(expiresAt)) {
      console.log('Expired Token, clear local storage');
      this.clearLocalStorage();
      return;
    }
    const name = localStorage.getItem('name');
    const email = localStorage.getItem('email');
    const picture = localStorage.getItem('picture');
    this.user = { accessToken, expiresAt, name, email, picture };
    setTimeout(() =>  store.dispatch(authActions.loginSuccess(this.user)));
  }

  logout() {
    this.clearLocalStorage();
    // navigate to the home route
    this.webAuth.logout({returnTo: authConfig.homeUrl});
  }

  clearLocalStorage() {
    this.user = null;
    // Clear access token and ID token from local storage
    localStorage.removeItem('access_token');
    localStorage.removeItem('expires_at');
    localStorage.removeItem('name');
    localStorage.removeItem('email');
    localStorage.removeItem('picture');
  }

  isTokenValid(expiresAt) {
    // Check whether the current time is past the
    // access token's expiry time
    let expiry = JSON.parse(expiresAt);
    return new Date().getTime() < expiry;
  }

  authHeader() {
    const user = _.get(store.getState(), "authentication.user");
    if (user && this.isTokenValid(user.expiresAt)) {
      return `Bearer ${user.accessToken}`;
    }
    return null;
  }

  getHeader = () => {
    const header = this.authHeader();
    if (!header) {
      console.log("apiCaller: Authentication failed");
      const error = {
        code: authConstants.AUTHENTICATION_FAILURE,
        message: "Authentication failed"
      };
      return { error };
    }
    return { header };
  }

  isAuthenticated() {
    const user = _.get(store.getState(), "authentication.user");
    if (user) {
      if (this.isTokenValid(user.expiresAt)) {
        return true;
      } else {
        console.log('Token expired, dispatch-->logout');
        setTimeout(() => store.dispatch(authActions.logout()));
      }
    }
    return false;
  }

}
const auth = new Auth();
export const authService = auth;
