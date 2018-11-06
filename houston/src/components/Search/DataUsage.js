import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { authActions, pseudoActions } from '../../actions';
import { Panel } from 'react-bootstrap';

const DataUsage = props => {
  return (
    <Panel>
      <Panel.Heading>Data balance</Panel.Heading>
      <Panel.Body>
        <samp>{`Remaining ${props.balance}`}</samp>
      </Panel.Body>
    </Panel>
  );
}

DataUsage.propTypes = {
  loggedIn: PropTypes.bool,
  pseudonym: PropTypes.object,
  balance: PropTypes.string.isRequired
};

function humanReadableBytes(sizeInBytes) {
  var i = -1;
  var byteUnits = ['KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
  do {
    sizeInBytes = sizeInBytes / 1024;
    i++;
  } while (sizeInBytes > 1024);
  return `${Math.max(sizeInBytes, 0.1).toFixed(1)} ${byteUnits[i]}`;
}

function mapStateToProps(state) {
  const { loggedIn } = state.authentication;
  const { pseudonym } = state;
  const balance = humanReadableBytes(1024 * 1024 * 1024 * 2);
  return {
    loggedIn,
    pseudonym,
    balance
  };
}
const mapDispatchToProps = {
  login: authActions.login,
  getPseudonym: pseudoActions.getPseudonym
}
export default connect(mapStateToProps, mapDispatchToProps)(DataUsage);
