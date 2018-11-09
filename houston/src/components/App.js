import React, { Component } from 'react';
import { Link } from 'react-router-dom';
import { connect } from 'react-redux';
import {
  Button,
  Collapse,
  Navbar,
  NavbarToggler,
  NavbarBrand,
  Nav,
  NavItem,
  NavLink,
  UncontrolledDropdown,
  DropdownToggle,
  DropdownMenu,
  DropdownItem
} from 'reactstrap';

import { authActions } from '../actions';

class App extends Component {
  constructor(props, context) {
    super(props, context);
    this.state = {
      isOpen: false
    };
  }

  goTo(route) {
    this.props.history.replace(`/${route}`)
  }

  login() {
    this.props.login();
  }

  logout() {
    this.props.logout();
  }

  toggle = () => {
    this.setState({
      isOpen: !this.state.isOpen
    });
  }

  renderMenu() {
    const { props } = this;
    const userName = props.user ? props.user.name + ' : ' + props.user.email : '';
    return (
      <Collapse isOpen={this.state.isOpen} navbar>
        <Nav className="ml-auto" navbar>
          <NavItem>
            <NavLink tag={Link} href="/search" to="/search">Search</NavLink>
          </NavItem>
          <NavItem>
            <NavLink tag={Link} href="/notifications" to="/notifications">Notifications</NavLink>
          </NavItem>
          <UncontrolledDropdown nav inNavbar>
            <DropdownToggle nav caret>
              Options
          </DropdownToggle>
            <DropdownMenu right>
              <DropdownItem>
                <NavLink href="" onClick={(e) => { e.preventDefault(); props.logout(); }}>Logout</NavLink>
              </DropdownItem>
            </DropdownMenu>
          </UncontrolledDropdown>
        </Nav>
      </Collapse>
    );
  }

  render() {
    const { props } = this
    const isAuthenticated = props.loggedIn || false;
    const userName = props.user ? props.user.name + ' : ' + props.user.email : '';

    return (
      <div>
        <Navbar light expand="md">
          <NavbarBrand>
            <img src="redotter.png" style={{ width: 32, height: 32, marginTop: 0 }} />
          </NavbarBrand>
          <Nav>
            <NavItem>
              {userName}
            </NavItem>
          </Nav>
          {
            !isAuthenticated && (
              <Button color="outline-primary" onClick={this.login.bind(this)}>Log In</Button>
            )
          }
          {
            isAuthenticated && (this.renderMenu())
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
  login: authActions.login,
  logout: authActions.logout
}

const connectedApp = connect(mapStateToProps, mapDispatchToProps)(App);
export default connectedApp;
