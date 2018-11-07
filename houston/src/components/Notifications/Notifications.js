import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { authActions, subscriberActions } from '../../actions';
import { TextForm } from './TextForm';
import { Panel } from 'react-bootstrap';

class Notifications extends React.Component {

  onSubmit = (text) => {
    //handle form processing here....
    console.log("Search On Submit")
    this.props.getSubscriberAndBundles(text)
  }

  render() {
    return (
      <div className="container">
      <Panel>
      <Panel.Heading>Global Push Notifications.</Panel.Heading>
      <Panel.Body>
        <TextForm onSubmit = {this.onSubmit} />
        </Panel.Body>
        </Panel>
        <Panel>
      <Panel.Heading>Global Emails</Panel.Heading>
      <Panel.Body>
        <TextForm onSubmit = {this.onSubmit} />
        </Panel.Body>
        </Panel>
      </div>
    );
  }
}

Notifications.propTypes = {
  loggedIn: PropTypes.bool,
  pseudonym: PropTypes.object,
  profile: PropTypes.object,
};

function mapStateToProps(state) {
  const { subscriber } = state;
  return {
    profile: subscriber
  };
}
const mapDispatchToProps = {
  login: authActions.login,
  getSubscriberAndBundles: subscriberActions.mockGetSubscriberAndBundles
}
export default connect(mapStateToProps, mapDispatchToProps)(Notifications);
