import React from 'react';
import { Card, CardBody, CardTitle } from 'reactstrap';

import DataUsage from "./DataUsage";
import NotificationEditor from '../Notifications/NotificationEditor';
import Profile from "./Profile";
import PaymentHistory from "./PaymentHistory";

class SearchResults extends React.Component {
  render() {
    return (
      <div className="container">
        <Profile />
        <br />
        <DataUsage />
        <br />
        <Card>
          <CardBody>
            <CardTitle>Push Notifications</CardTitle>
            <NotificationEditor
              submitLabel="Send a message"
              titleLabel="Title"
              messageLabel="Message"
            />
          </CardBody>
        </Card>
        <br />
        <PaymentHistory />
      </div>
    );
  }
}

export default SearchResults;
