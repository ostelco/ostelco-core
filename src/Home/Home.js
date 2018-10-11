import React, { Component } from 'react';
import { connect } from 'react-redux';
import *as _ from 'lodash';
import { userActions } from '../actions';

class Home extends Component {
  login() {
    this.props.dispatch(userActions.login());
  }

  testAPI() {
    let accessToken = this.props.user.accessToken;
    var url = 'https://houston-api.dev.ostelco.org/pseudonym/current/4790300168';
    console.log(`testAPI with header : Bearer ${accessToken}`)
    fetch(url, {
      method: 'GET', // or 'PUT'
      headers:{
        'authorization': `Bearer ${accessToken}`
      }
    }).then(res => res.json())
    .then(response => console.log('Success:', JSON.stringify(response)))
    .catch(error => console.error('Error:', error));
  }

  render() {
    const { isAuthenticated } = this.props;
    return (
      <div className="container">
        {
          isAuthenticated && (
              <h4>
                You are logged in!<br></br><br></br>
                Test the Houston API. {' '}
                <a
                  style={{ cursor: 'pointer' }}
                  onClick={this.testAPI.bind(this)}
                >
                  Click here
                </a>
                {' '}to test.
              </h4>
            )
        }
        {
          !isAuthenticated && (
            <h4>
              You are not logged in! Please{' '}
              <a
                style={{ cursor: 'pointer' }}
                onClick={this.login.bind(this)}
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
}
function mapStateToProps(state) {
  const { loggingIn, user } = state.authentication;
  const isAuthenticated = _.get(state, "authentication.loggedIn", false);
  return {
      loggingIn,
      user,
      isAuthenticated
  };}

const connectedHome = connect(mapStateToProps)(Home);
export default connectedHome;

