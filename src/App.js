import React, { Component } from 'react';
import { connect } from 'react-redux';
import { Navbar, Button } from 'react-bootstrap';
import * as _ from 'lodash';

import { userActions } from './actions';
import './App.css';

class App extends Component {
  goTo(route) {
    this.props.history.replace(`/${route}`)
  }

  login() {
    this.props.dispatch(userActions.login());
  }

  logout() {
    this.props.dispatch(userActions.logout());
  }

  render() {
    console.log(JSON.stringify(this.props));
    const { isAuthenticated } = this.props;

    return (
      <div>
        <Navbar fluid>
          <Navbar.Header>
            <Navbar.Brand>
              <a href="#">
              <img src="redotter.png" style={{width:100, marginTop: 5}} />
              Houston
              </a>
            </Navbar.Brand>
            <Button
              bsStyle="primary"
              className="btn-margin"
              onClick={this.goTo.bind(this, 'home')}
            >
              Home
            </Button>
            {
              !isAuthenticated && (
                  <Button
                    id="qsLoginBtn"
                    bsStyle="primary"
                    className="btn-margin"
                    onClick={this.login.bind(this)}
                  >
                    Log In
                  </Button>
                )
            }
            {
              isAuthenticated && (
                  <Button
                    id="qsLogoutBtn"
                    bsStyle="primary"
                    className="btn-margin"
                    onClick={this.logout.bind(this)}
                  >
                    Log Out
                  </Button>
                )
            }
          </Navbar.Header>
        </Navbar>
      </div>
    );
  }
}

function mapStateToProps(state) {
  console.log(JSON.stringify(state));
  const { loggingIn } = state.authentication;
  const isAuthenticated = _.get(state, "authentication.loggedIn", false);
  return {
      loggingIn,
      isAuthenticated
  };
}

const connectedApp = connect(mapStateToProps)(App);
export default connectedApp;
