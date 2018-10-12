import { CALL_API } from '../helpers';
import { pseudoConstants } from '../constants';

const fetchPseudonym = (msisdn) => ({
  [CALL_API]: {
    types: [pseudoConstants.PSEUDONYM_REQUEST, pseudoConstants.PSEUDONYM_SUCCESS, pseudoConstants.PSEUDONYM_FAILURE],
    endpoint: `pseudonym/current/${msisdn}`,
    method: 'GET'
  }
});

const getPseudonym = (msisdn) => (dispatch, getState) => {
  console.log("getPseudonym =", msisdn);
  return dispatch(fetchPseudonym(msisdn));
}

export const pseudoActions = {
  getPseudonym
};
