import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { Button, Panel, Grid, Row, Col } from 'react-bootstrap';

const Profile = props => {
  console.log(JSON.stringify(props.profile));
  if (!props.profile.name) return null;
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
  profile: PropTypes.shape({
    name: PropTypes.string,
    email:PropTypes.string,
    address: PropTypes.string
  }),
};

function mapStateToProps(state) {
  console.log("Profile mapStateToProps", JSON.stringify(state))
  const { subscriber } = state;
  return {
    profile: subscriber
  };
}
export default connect(mapStateToProps)(Profile);
