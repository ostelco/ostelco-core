
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

const DEPLOYED_PROD_AUTH_CONFIG = {
  domain: 'redotter-admin.eu.auth0.com',
  clientId: 'kD1KTanjQlpBUPwB0jiYq4kqWBdOa7AZ',
  callbackUrl: 'https://redotter-admin.firebaseapp.com/callback',
  homeUrl: 'https://redotter-admin.firebaseapp.com'
};

export function getAuthConfig() {
  if (process.env.REACT_APP_DEPLOYMENT_ENV === "development") {
    return DEPLOYED_DEV_AUTH_CONFIG;
  } else if (process.env.REACT_APP_DEPLOYMENT_ENV === "production" ||
    process.env.NODE_ENV === "production") {
    return DEPLOYED_PROD_AUTH_CONFIG;
  } else {
    return DEV_AUTH_CONFIG;
  }
}

export function getAPIRoot() {
  const DEV_API_ROOT = 'https://houston-api.dev.oya.world/support/';
  const PROD_API_ROOT = 'https://houston-api.oya.world/support/';

  if (process.env.REACT_APP_DEPLOYMENT_ENV === "development" ||
    process.env.NODE_ENV === "development") {
    return DEV_API_ROOT;
  } else if (process.env.REACT_APP_DEPLOYMENT_ENV === "production" ||
    process.env.NODE_ENV === "production") {
    return PROD_API_ROOT;
  } else {
    return DEV_API_ROOT;
  }
}
