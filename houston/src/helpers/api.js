import _ from 'lodash';
import { getAPIRoot } from '../services/config-variables';

const API_ROOT = getAPIRoot();

let authHeaderResolver = null;
export function setAuthResolver(getterFunc) {
  authHeaderResolver = getterFunc;
}

export function createParams(params) {
  const array = _.toPairs(params);
  const kvParams = _.map(array, (kv) => {
    return `${kv[0]}=${encodeURIComponent(kv[1])}`;
  });
  return (array.length ? `?${kvParams.join('&')}` : '');
}


const apiCaller = async (endpoint, method, body, allowEmptyResponse, params = []) => {
  let fullUrl = (endpoint.indexOf(API_ROOT) === -1) ? API_ROOT + endpoint : endpoint;

  if (typeof params === 'object') {
    fullUrl += createParams(params);
  } else if (typeof params === 'string') {
    fullUrl += params;
  }

  //console.log('API URL:', fullUrl);
  if (authHeaderResolver === null) {
    console.log("apiCaller: authHeaderResolver not set");
    return Promise.reject();
  }
  const auth = authHeaderResolver();
  if (auth.error) {
    return Promise.reject(auth.error);
  }
  let options = {
    method,
    headers: {
      Accept: 'application/json',
      Authorization: auth.header,
      //TODO: remove this when we are ready to deploy in all versions of prime
      //"x-mode": "prime-direct"
    }
  };
  if (body) {
    options.body = body;
    options.headers['content-type'] = 'application/json';
  }
  //console.log('Calling', fullUrl, options);
  return fetch(fullUrl, options)
    .then(response => {
      return response.text().then(text => {
        //console.log(`Response text for -> ${fullUrl} ==> [${text}]`);
        let json = null;
        let exception = null;
        // Capture any JSON parse exception.
        try {
          json = JSON.parse(text);
        } catch (e) {
          exception = e;
        }
        // Ignore exceptions for allowed empty responses
        if (allowEmptyResponse && allowEmptyResponse === true) {
          return {};
        }
        // Rethrow any parse exceptions.
        if (exception !== null) {
          throw exception;
        }
        if (!response.ok) {
          return Promise.reject(json);
        }
        return json;
      });
    });
};


export function transformError(errorObj) {
  if (errorObj.errors) {
    if (Array.isArray(errorObj.errors)) {
      return errorObj.errors.join(', ');
    } else {
      return errorObj.errors.toString();
    }
  } else if (errorObj.message) {
    return errorObj.message.toString();
  } else if (errorObj.error) {
    return errorObj.error.toString();
  } else if (typeof errorObj === 'string') {
    return errorObj;
  } else {
    return 'Something bad happened';
  }
}

// Action key that carries API call info interpreted by this Redux middleware.
export const CALL_API = 'Call API';

// A Redux middleware that interprets actions with CALL_API info specified.
// Performs the call and promises when such actions are dispatched.
export default (store) => (next) => (action) => {
  const callAPI = action[CALL_API];
  if (typeof callAPI === 'undefined') {
    return next(action);
  }

  let { endpoint } = callAPI;
  const { actions, method, body, allowEmptyResponse, params = [] } = callAPI;

  if (typeof endpoint === 'function') {
    endpoint = endpoint(store.getState());
  }

  if (typeof endpoint !== 'string') {
    throw new Error('Specify a string endpoint URL.');
  }
  if (!Array.isArray(actions) || actions.length !== 3) {
    throw new Error('Expected an array of three actions.');
  }
  if (!actions.every((action) => typeof action === 'function')) {
    throw new Error('Expected actions to be functions.');
  }

  const [request, success, failure] = actions;
  next(request());

  return apiCaller(endpoint, method, body, allowEmptyResponse, params).then(
    (response) => next(success(response)),
    (error) => {
      next(failure({
        errorObj: error,
        error: transformError(error)
      }));
      throw new Error(transformError(error));
    }
  );
}

