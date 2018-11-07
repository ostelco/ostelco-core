import React, { Component } from 'react';
import { Link } from 'react-router-dom';
import { connect } from 'react-redux';
import { Navbar, Button, NavItem, Nav, MenuItem, NavDropdown } from 'react-bootstrap';

import { authActions } from '../actions';
import './App.css';

class App extends Component {
  goTo(route) {
    this.props.history.replace(`/${route}`)
  }

  login() {
    this.props.login();
  }

  logout() {
    this.props.logout();
  }

  render() {
    const { props } = this
    const isAuthenticated = props.loggedIn || false;
    const userName = props.user ? props.user.name + ' : ' + props.user.email : '';
    return (
      <div>
        <Navbar fluid>
          <Navbar.Header>
            <Navbar.Brand>
                <img src="redotter.png" style={{ width: 80, height: 80, marginTop: -10 }} />
            </Navbar.Brand>
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
          </Navbar.Header>
          {
            isAuthenticated && (
              <Navbar.Collapse>
                <Nav>
                <NavItem componentClass={Link} href="/search" to="/search" active={window.location.pathname === '/search'}>Search</NavItem>
                <NavItem componentClass={Link} href="/notify" to="/notify" active={window.location.pathname === '/notify'}>Notifications</NavItem>
                  <NavDropdown eventKey={3} title="Dropdown" id="basic-nav-dropdown">
                    <MenuItem eventKey={3.1}>Action</MenuItem>
                    <MenuItem eventKey={3.2}>Another action</MenuItem>
                    <MenuItem eventKey={3.3}>Something else here</MenuItem>
                    <MenuItem divider />
                    <MenuItem eventKey={3.3}>Separated link</MenuItem>
                  </NavDropdown>
                </Nav>
                <Navbar.Text pullRight>
                  <Navbar.Link href="" onClick={(e) => { e.preventDefault(); props.logout(); }}>
                    {' '+ userName + ' '}
                  </Navbar.Link>
                </Navbar.Text>
              </Navbar.Collapse>
            )
          }
        </Navbar>
      </div>
    );
  }
}

function mapStateToProps(state) {
  const { loggedIn, user } = state.authentication;
  return {
    loggedIn,
    user
  };
}
const mapDispatchToProps = {
  login: authActions.login,
  logout: authActions.logout
}

const connectedApp = connect(mapStateToProps, mapDispatchToProps)(App);
export default connectedApp;
