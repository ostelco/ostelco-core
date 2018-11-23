import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';

const Home = props => {
  return (
    <div className="container">
      {
        !props.isAuthenticated && (
          <h4>
            You are not logged in! Please Log In to continue.
          </h4>
        )
      }
    </div>
  );
}

Home.propTypes = {
  isAuthenticated: PropTypes.bool.isRequired
};

function mapStateToProps(state) {
  const { loggedIn } = state.authentication;
  return { isAuthenticated: loggedIn ? true: false };
}

export default connect(mapStateToProps)(Home);
