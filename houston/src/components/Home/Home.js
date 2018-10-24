import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { authActions, pseudoActions } from '../../actions';
import GraphiQL from 'graphiql';
import './graphiql.css';

function graphQLFetcher(graphQLParams) {
  return fetch('http://localhost:62002' + '/graphql', {
    method: 'post',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(graphQLParams),
  }).then(response => response.json());
}

const Home = props => {
  const isAuthenticated = props.loggedIn || false;
  const pseudonym = JSON.stringify(props.pseudonym);
  return (
    <div className="container">
      {
        isAuthenticated && (
          <h4>
            You are logged in!<br /><br />
            Test the Houston API. {' '}
            <a
              style={{ cursor: 'pointer' }}
              onClick={() => { props.getPseudonym('4790300168') }}
            >
              Click here
                </a>
            {' '}to test.
              <br /><br />
            Last Result {`   ${pseudonym}`}
            <GraphiQL fetcher={graphQLFetcher} height="500px"/>
          </h4>
        )
      }
      {
        !isAuthenticated && (
          <h4>
            You are not logged in! Please{' '}
            <a
              style={{ cursor: 'pointer' }}
              onClick={() => { props.login() }}
            >
              Log In
              </a>
            {' '}to continue.
            </h4>
        )
      }
    </div>
  );
}

Home.propTypes = {
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
export default connect(mapStateToProps, mapDispatchToProps)(Home);
