import React, { Component } from 'react';

class Home extends Component {
  login() {
    this.props.auth.login();
  }

  testAPI() {
    let accessToken = localStorage.getItem('access_token');
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
    const { isAuthenticated } = this.props.auth;
    return (
      <div className="container">
        {
          isAuthenticated() && (
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
          !isAuthenticated() && (
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

export default Home;
