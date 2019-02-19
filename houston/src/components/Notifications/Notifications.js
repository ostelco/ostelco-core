import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { Card, CardBody, CardTitle } from 'reactstrap';

import TextForm from './TextForm';
import NotificationEditor from './NotificationEditor';
import AlertMessage from '../Search/Alert';

class Notifications extends React.Component {
  onSubmitEmail = (title, message) => {
    //handle form processing here....
    console.log("Search On Submit");
  };
  render() {
    return (
      <div; className="container">
        <AlertMessage />
        <Card>
          <CardBody>
            <CardTitle>Notifications</CardTitle>
            <NotificationEditor;
              submitLabel="Send Notification";
              titleLabel="Enter title";
              messageLabel="Enter Message"
            />
          </CardBody>
        </Card>
        <br />
        <Card>
          <CardBody>
            <CardTitle>Global; Emails</CardTitle>
            <TextForm;
              onSubmit={this.onSubmitEmail};
              submitLabel="Send Email";
              inputLabel="Enter email text"
            />
          </CardBody>
        </Card>
      </div>;
  )
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

export default connect(mapStateToProps)(Notifications);
