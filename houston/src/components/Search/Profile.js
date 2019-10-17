import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { Col, Row, Card, CardBody, CardTitle, Button } from 'reactstrap';

import { subscriberActions } from '../../actions/subscriber.actions';
import Subscription from './Subscription';
import WarningModal from '../Shared/WarningModal';

class Profile extends React.Component {
  constructor(props, context) {
    super(props, context);
    this.state = {
      showWarning: false
    };
  }

  handleCloseModal = () => {
    this.setState({
      showWarning: false
    });
  }

  handleShowModal = () => {
    this.setState({
      showWarning: true
    });
  }

  handleConfirmModal = () => {
    this.props.deleteUser();
    this.handleCloseModal();
  }

  render() {
    const modalHeading = `Confirm remove user`
    const modalText = `Do you really want to remove this user?`
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
            <Col xs={2} md={2}>{'ID:'}</Col>
            <Col xs={12} md={8}>{`${props.profile.id}`}</Col>
          </Row>
          <br />
          <Row>
            <Col xs={6} md={4}>
              <Button color="danger" onClick={this.handleShowModal}>{'Remove User'}</Button>
            </Col>
          </Row>
          <hr />
          {listItems}
          <WarningModal
            heading={modalHeading}
            dangerStyle={true}
            warningText={modalText}
            show={this.state.showWarning}
            handleConfirm={this.handleConfirmModal}
            handleClose={this.handleCloseModal} />
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
  deleteUser:PropTypes.func.isRequired
};

function mapStateToProps(state) {
  const { subscriptions } = state;
  const { customer } = state
  return {
    profile: customer,
    subscriptions
  };
}
const mapDispatchToProps = {
  deleteUser: subscriberActions.deleteUser
}
export default connect(mapStateToProps, mapDispatchToProps)(Profile);
