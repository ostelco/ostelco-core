import React from 'react';
import { Card, CardBody, CardTitle, Nav, NavItem, NavLink, TabContent, TabPane } from 'reactstrap';
import classnames from 'classnames';

import Context from "./Context";
import DataUsage from "./DataUsage";
import NotificationEditor from '../Notifications/NotificationEditor';
import Profile from "./Profile";
import PaymentHistory from "./PaymentHistory";
import AuditLogs from "./AuditLogs";

class SearchResults extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      activeTab: '1'
    };
  }

  toggle = (tab) => {
    if (this.state.activeTab !== tab) {
      this.setState({
        activeTab: tab
      });
    }
  }
  render() {
    return (
      <div className="container">
        <Nav tabs>
          <NavItem>
            <NavLink
              className={classnames({ active: this.state.activeTab === '1' })}
              onClick={() => { this.toggle('1'); }}
            >
              Profile
            </NavLink>
          </NavItem>
          <NavItem>
            <NavLink
              className={classnames({ active: this.state.activeTab === '2' })}
              onClick={() => { this.toggle('2'); }}
            >
              Purchases
            </NavLink>
          </NavItem>
          <NavItem>
            <NavLink
              className={classnames({ active: this.state.activeTab === '3' })}
              onClick={() => { this.toggle('3'); }}
            >
              Context
            </NavLink>
          </NavItem>
          <NavItem>
            <NavLink
              className={classnames({ active: this.state.activeTab === '4' })}
              onClick={() => { this.toggle('4'); }}
            >
              Audit Logs
            </NavLink>
          </NavItem>
        </Nav>
        <TabContent activeTab={this.state.activeTab}>
          <TabPane tabId="1">
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
          </TabPane>
          <TabPane tabId="2">
            <PaymentHistory />
          </TabPane>
          <TabPane tabId="3">
            <Context />
          </TabPane>
          <TabPane tabId="4">
            <AuditLogs />
          </TabPane>
        </TabContent>
      </div>
    );
  }
}

export default SearchResults;
