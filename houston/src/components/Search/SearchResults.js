import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';

import { authActions, pseudoActions } from '../../actions';
import DataUsage from "./DataUsage";
import Profile from "./Profile";
import PaymentHistory from "./PaymentHistory";

class SearchResults extends React.Component {
  render() {
    return (
      <div className="container">
        <Profile />
        <br />
        <DataUsage />
        <br />
        <PaymentHistory />
      </div>
    );
  }
}

SearchResults.propTypes = {
  loggedIn: PropTypes.bool,
  pseudonym: PropTypes.object,
};

function mapStateToProps(state) {
  const { loggedIn } = state.authentication;
  const { pseudonym } = state;

  return {
    loggedIn,
    pseudonym,
  };
}
const mapDispatchToProps = {
  login: authActions.login,
  getPseudonym: pseudoActions.getPseudonym
}
export default connect(mapStateToProps, mapDispatchToProps)(SearchResults);
