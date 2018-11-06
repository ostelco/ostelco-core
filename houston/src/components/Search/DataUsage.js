import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { authActions, pseudoActions } from '../../actions';
import { Panel } from 'react-bootstrap';

const DataUsage = props => {
  return (
    <Panel>
      <Panel.Heading>Data Usage</Panel.Heading>
      <Panel.Body> Used 3.2 of 5GB [Remaining 1.8GB]
      </Panel.Body>
    </Panel>
  );
}

DataUsage.propTypes = {
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
export default connect(mapStateToProps, mapDispatchToProps)(DataUsage);
