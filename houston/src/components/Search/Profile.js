import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { authActions, pseudoActions } from '../../actions';
import { Button, Panel, Grid, Row, Col } from 'react-bootstrap';

const Profile = props => {
  return (
    <Panel>
      <Panel.Heading>User Profile</Panel.Heading>
      <Panel.Body>
        <Grid>
          <Row className="show-grid">
            <Col xs={1} md={1}>
              <samp>{'Name:'}</samp>
            </Col>
            <Col xs={12} md={8}>
              <samp>{`${props.profile.name}`}</samp>
            </Col>
            </Row>
            <Row className="show-grid">
            <Col xs={1} md={1}>
              <samp>{'Email:'}</samp>
            </Col>
            <Col xs={12} md={8}>
              <samp>{`${props.profile.email}`}</samp>
            </Col>
          </Row>

          <Row className="show-grid">
            <Col xs={1} md={1}>
              <samp>{'Address:'}</samp>
            </Col>
            <Col xs={12} md={8}>
              <samp>{`${props.profile.address}`}</samp>
            </Col>
          </Row>
          <br />
          <Row className="show-grid">
            <Col xs={12} md={6}>
            <samp>{'Block '}<Button bsStyle="link">{'current SIM card'}</Button></samp>
            </Col>
          </Row>
          <Row className="show-grid">
            <Col xs={12} md={6}>
            <samp>{'Order '}<Button bsStyle="link">{'new SIM card'}</Button></samp>
            </Col>
          </Row>
        </Grid>
      </Panel.Body>
    </Panel>
  );
}

Profile.propTypes = {
  loggedIn: PropTypes.bool,
  pseudonym: PropTypes.object,
  profile: PropTypes.shape({
    name: PropTypes.string,
    email:PropTypes.string,
    address: PropTypes.string
  }).isRequired,
};

function mapStateToProps(state) {
  const { loggedIn } = state.authentication;
  const { pseudonym } = state;
  const profile = {
    name: "Shane Warne",
    email: "shane@icc.org",
    address: "4, Leng Kee Road,  #06-07 SiS Building, Singapore 159088"
  }
  return {
    loggedIn,
    pseudonym,
    profile
  };
}
const mapDispatchToProps = {
  login: authActions.login,
  getPseudonym: pseudoActions.getPseudonym
}
export default connect(mapStateToProps, mapDispatchToProps)(Profile);
