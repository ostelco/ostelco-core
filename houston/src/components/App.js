import React, { Component } from 'react';
import { Link } from 'react-router-dom';
import { connect } from 'react-redux';
import { Button, Collapse, Navbar, NavbarToggler, NavbarBrand, Nav, NavItem, NavLink } from 'reactstrap';

import { authActions } from '../actions/auth.actions';
import './App.css'

class App extends Component {
  constructor(props, context) {
    super(props, context);
    this.state = {
      isOpen: false
    };
  }

  toggle = () => {
    this.setState({
      isOpen: !this.state.isOpen
    });
  }

  renderMenu() {
    const { props } = this;
    return (
      <Collapse isOpen={this.state.isOpen} navbar>
        <Nav className="ml-auto" navbar>
          <NavItem>
            <NavLink tag={Link} href="/" to="/">Search</NavLink>
          </NavItem>
          <NavItem>
            <NavLink tag={Link} href="/notifications" to="/notifications">Notifications</NavLink>
          </NavItem>
          <NavItem>
            <NavLink tag={Link} href="" to="/" onClick={(e) => { e.preventDefault(); props.logout(); }}>Logout</NavLink>
          </NavItem>
        </Nav>
      </Collapse>
    );
  }

  render() {
    const { props } = this
    const loggedIn = props.loggedIn || false;
    const userName = props.user ? props.user.name + ' : ' + props.user.email : '';

    return (
      <div>
        <Navbar light expand="md">
          <NavbarBrand>
            <img src="redotter.png" alt="Red Otter" style={{ width: 60, height: 60, marginTop: -10 }} />
          </NavbarBrand>
          <Nav>
            <NavItem>
              {userName}
            </NavItem>
          </Nav>
          {
            !loggedIn && (
              <Button color="outline-primary" onClick={props.login}>Log In</Button>
            )
          }
          {
            loggedIn && (this.renderMenu())
          }
          <NavbarToggler onClick={this.toggle} />
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
  login: authActions.loginRequest,
  logout: authActions.logout
}

const connectedApp = connect(mapStateToProps, mapDispatchToProps)(App);
export default connectedApp;
