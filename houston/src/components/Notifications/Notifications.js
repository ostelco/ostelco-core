import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { Card, CardBody, CardTitle } from 'reactstrap';

import { authActions, subscriberActions } from '../../actions';
import TextForm from './TextForm';

class Notifications extends React.Component {

  onSubmit = (text) => {
    //handle form processing here....
    console.log("Search On Submit")
    this.props.getSubscriberAndBundles(text)
  }
  render() {
    return (
      <div className="container">
        <Card>
          <CardBody>
            <CardTitle>Global Push Notifications.</CardTitle>
            <TextForm
              onSubmit={this.onSubmit}
              submitLabel="Send Notification"
              inputLabel="Enter notification"
            />
          </CardBody>
        </Card>
        <br />
        <Card>
          <CardBody>
            <CardTitle>Global Emails</CardTitle>
            <TextForm
              onSubmit={this.onSubmit}
              submitLabel="Send Email"
              inputLabel="Enter email text"
            />
          </CardBody>
        </Card>
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
