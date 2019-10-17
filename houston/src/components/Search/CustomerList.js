import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { Col, Row, Card, CardBody, Button } from 'reactstrap';

import { subscriberActions } from '../../actions/subscriber.actions';

export const CustomerRow = props => {

  function onSelect(e) {
    e.preventDefault();
    console.log(`Selecting customer with id ${props.customer.id}`);
    props.selectCustomer(props.customer);
  }

  return (
      <div>
      <Row>
        <Col xs={2} md={2}>{'Name:'}</Col>
        <Col xs={12} md={8}>{`${props.customer.nickname}`}</Col>
      </Row>
      <Row>
        <Col xs={2} md={2}>{'Email:'}</Col>
        <Col xs={12} md={8}>{`${props.customer.contactEmail}`}</Col>
      </Row>
      <Row>
        <Col xs={2} md={2}>{'ID:'}</Col>
        <Col xs={12} md={8}>{`${props.customer.id}`}</Col>
      </Row>
      <br />
      <Row>
        <Col xs={6} md={4}>
          <Button color="light" onClick={onSelect}>{'Show details'}</Button>
        </Col>
      </Row>
      </div>);
}

CustomerRow.propTypes = {
  customer: PropTypes.shape({
    id: PropTypes.string,
    nickname: PropTypes.string,
    contactEmail: PropTypes.string,
  }),
  selectCustomer: PropTypes.func.isRequired
};

export const CustomerList = props => {
  // If customer is set, remove the list.
  console.log(JSON.stringify(props))
  if (props.customer.id || !Array.isArray(props.subscribers)) {
    return null;
  }
  let listItems = null;
  if (Array.isArray(props.subscribers)) {
    listItems = props.subscribers.map((customer, index) =>
      <div key={index}>
        <CustomerRow customer={customer} selectCustomer={props.selectCustomer} key={index} />
        <hr />
      </div>
    );
  }
  return (
    <div>
      <h6>Found following matching records... </h6>
      <Card>
      <CardBody>
        {listItems}
        </CardBody>
      </Card>
    </div>
  );
}

CustomerList.propTypes = {
  subscribers: PropTypes.oneOfType([
    PropTypes.object,
    PropTypes.array
  ]),
  customer: PropTypes.object,
  selectCustomer: PropTypes.func.isRequired
};

function mapStateToProps(state) {
  const { subscribers, customer } = state

  return {
    subscribers,
    customer
  };
}
const mapDispatchToProps = {
  selectCustomer: subscriberActions.selectCustomer
}
export default connect(mapStateToProps, mapDispatchToProps)(CustomerList);
