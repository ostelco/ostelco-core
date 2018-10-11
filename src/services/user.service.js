import config from 'config';
import { history, authHeader } from '../helpers';
import auth0 from 'auth0-js';
import { AUTH_CONFIG } from './auth0-variables';
import { userActions } from '../actions';

class Auth {
  auth0 = new auth0.WebAuth({
    domain: AUTH_CONFIG.domain,
    clientID: AUTH_CONFIG.clientId,
    redirectUri: AUTH_CONFIG.callbackUrl,
    responseType: 'token id_token',
    scope: 'openid',
    audience: 'http://google_api'
  });

  constructor() {
    console.log("Constructing Auth");
    this.user = {};
    this.login = this.login.bind(this);
    this.logout = this.logout.bind(this);
    this.handleAuthentication = this.handleAuthentication.bind(this);
    this.isAuthenticated = this.isAuthenticated.bind(this);
  }

  login() {
    this.auth0.authorize();
  }

  handleAuthentication(dispatch) {
    this.auth0.parseHash((err, authResult) => {
      if (authResult && authResult.accessToken) {
        this.setSession(authResult);
        history.replace('/home');
        dispatch(userActions.loginSuccess(this.user));
      } else if (err) {
        console.log(err);
        alert(`Error: ${err.error}. Check the console for further details.`);
        this.logout()
        dispatch(userActions.loginFailure(err));
      }
    });
  }

  setSession(authResult) {
    console.log(JSON.stringify(authResult));
    // Set the time that the access token will expire at
    let expiresAt = JSON.stringify((authResult.expiresIn * 1000) + new Date().getTime());
    localStorage.setItem('access_token', authResult.accessToken);
    localStorage.setItem('expires_at', expiresAt);
    // navigate to the home route
    history.replace('/home');
    const { accessToken } = authResult;
    this.user = { accessToken, expiresAt };
  }

  loadCurrentSession(dispatch) {
    const accessToken = localStorage.getItem('access_token');
    const expiresAt = localStorage.getItem('expires_at');
    this.user = { accessToken, expiresAt };
    if (this.isAuthenticated()) {
      dispatch(userActions.loginSuccess(this.user));
    } else {

    }
  }

  logout() {
    this.user = { };
    // Clear access token and ID token from local storage
    localStorage.removeItem('access_token');
    localStorage.removeItem('expires_at');
    // navigate to the home route
    history.replace('/home');
  }

  isAuthenticated() {
    // Check whether the current time is past the
    // access token's expiry time
    let expiresAt = JSON.parse(this.user.expiresAt);
    return new Date().getTime() < expiresAt;
  }
}
const auth = new Auth();

export default auth;
export const userService = auth;

function getAll() {
  // const requestOptions = {
  //   method: 'GET',
  //   headers: authHeader()
  // };

  // return fetch(`${config.apiUrl}/users`, requestOptions).then(handleResponse);
}
