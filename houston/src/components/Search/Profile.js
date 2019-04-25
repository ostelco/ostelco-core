import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { Col, Row, Card, CardBody, CardTitle } from 'reactstrap';

import Subscription from './Subscription';

class Profile extends React.Component {
  render() {
    const props = this.props;

    let listItems = null;
    if (Array.isArray(props.subscriptions.items)) {
      listItems = props.subscriptions.items.map((subscription, index) =>
        <div key={index}>
          <Subscription subscription={subscription}  key={index}/>
          <hr />
        </div>
      );
    }
    return (
      <Card>
        <CardBody>
          <CardTitle>User Profile</CardTitle>
          <Row>
            <Col xs={2} md={2}>{'Name:'}</Col>
            <Col xs={12} md={8}>{`${props.profile.nickname}`}</Col>
          </Row>
          <Row>
            <Col xs={2} md={2}>{'Email:'}</Col>
            <Col xs={12} md={8}>{`${props.profile.contactEmail}`}</Col>
          </Row>
          <Row>
            <Col xs={2} md={2}>{'Address:'}</Col>
            <Col xs={12} md={8}>{`${props.profile.address}`}</Col>
          </Row>
          <hr />
          {listItems}
        </CardBody>
      </Card>
    );
  }
}

Profile.propTypes = {
  profile: PropTypes.shape({
    nickname: PropTypes.string,
    contactEmail: PropTypes.string,
    address: PropTypes.string
  }),
  subscriptions: PropTypes.shape({
    items: PropTypes.array,
  }),
};

function mapStateToProps(state) {
  const { subscriber } = state;
  const { subscriptions } = state;
  return {
    profile: subscriber,
    subscriptions
  };
}
export default connect(mapStateToProps)(Profile);
