import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { Card, CardBody, CardTitle } from 'reactstrap';

import TextForm from './TextForm';

class Notifications extends React.Component {

  onSubmit = (text) => {
    //handle form processing here....
    console.log("Search On Submit")
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

export default connect(mapStateToProps)(Notifications);
