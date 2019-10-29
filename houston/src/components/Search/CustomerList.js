import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { Button, Card, CardBody, CardText, Col, Row } from 'reactstrap';
import Highlighter from "react-highlight-words";

import { subscriberActions } from '../../actions/subscriber.actions';

const Highlight = ({ children, highlightIndex }) => (
  <strong className="bg-warning text-dark">{children}</strong>
);

const convertToHighlightedText = (text, query) => {
  return (
    <Highlighter
      searchWords={[query]}
      autoEscape={true}
      caseSensitive={true}
      textToHighlight={text}
      highlightTag={Highlight}
    />);
}

export const CustomerRow = props => {

  let query = localStorage.getItem('searchedEmail')
  function onSelect(e) {
    e.preventDefault();
    console.log(`Selecting customer with id ${props.customer.id}`);
    props.selectCustomer(props.customer);
  }

  return (
    <Card>
      <CardBody>
        <CardText>
          {convertToHighlightedText(props.customer.nickname, query)}<br />
          {convertToHighlightedText(props.customer.contactEmail, query)}<br />
          {convertToHighlightedText(props.customer.id, query)}<br />
          <br />
          <Button color="primary" onClick={onSelect}>{'Show details'}</Button>
        </CardText>
      </CardBody>
    </Card>);
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
  if (props.customer.id || !Array.isArray(props.subscribers)) {
    return null;
  }
  let listItems = null;
  if (Array.isArray(props.subscribers)) {
    listItems = props.subscribers.map((customer, index) =>
      <div key={index}>
        <CustomerRow customer={customer} selectCustomer={props.selectCustomer} key={index} />
        <br />

      </div>
    );
  }
  return (
    <div>
      <h6>Found following matching records... </h6>
      {listItems}
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
