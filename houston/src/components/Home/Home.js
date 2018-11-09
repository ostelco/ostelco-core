import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';

const Home = props => {
  const isAuthenticated = props.loggedIn || false;
  return (
    <div className="container">
      {
        !isAuthenticated && (
          <h4>
            You are not logged in! Please Log In to continue.
          </h4>
        )
      }
    </div>
  );
}

Home.propTypes = {
  loggedIn: PropTypes.bool
};

function mapStateToProps(state) {
  const { loggedIn } = state.authentication;

  return {
    loggedIn
  };
}

export default connect(mapStateToProps)(Home);
