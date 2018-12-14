
const DEV_AUTH_CONFIG = {
  domain: 'redotter-admin-dev.eu.auth0.com',
  clientId: '9DgdUDakjmn3O00NkDKna0YAsZanYqof',
  callbackUrl: 'http://localhost:3000/callback',
  homeUrl: 'http://localhost:3000'
};

const DEPLOYED_DEV_AUTH_CONFIG = {
  domain: 'redotter-admin-dev.eu.auth0.com',
  clientId: '9DgdUDakjmn3O00NkDKna0YAsZanYqof',
  callbackUrl: 'https://redotter-admin-dev.firebaseapp.com/callback',
  homeUrl: 'https://redotter-admin-dev.firebaseapp.com'
};

export function getAuthConfig() {
  if (process.env.REACT_APP_DEPLOYMENT_ENV === "development") {
    return DEPLOYED_DEV_AUTH_CONFIG;
  } else if (process.env.NODE_ENV === "development") {
    return DEV_AUTH_CONFIG;
  } else {
    return DEV_AUTH_CONFIG;
  }
}

export function getAPIRoot() {
  const API_ROOT = 'https://houston-api.dev.ostelco.org/';
  if (process.env.REACT_APP_DEPLOYMENT_ENV === "development") {
    return API_ROOT;
  } else if (process.env.NODE_ENV === "development") {
    return API_ROOT;
  } else {
    return API_ROOT;
  }
}
