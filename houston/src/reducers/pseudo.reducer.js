import { pseudoConstants } from '../constants';

export function pseudonym(state = {}, action) {
  switch (action.type) {
    case pseudoConstants.PSEUDONYM_REQUEST:
      return {
        loading: true
      };
    case pseudoConstants.PSEUDONYM_SUCCESS:
      return {
        item: action.response
      };
    case pseudoConstants.PSEUDONYM_FAILURE:
      return {
        error: action.error
      };
    default:
      return state
  }
}
