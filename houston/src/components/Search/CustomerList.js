import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { Button, Card, CardBody, CardText } from 'reactstrap';
import Highlighter from "react-highlight-words";

import { subscriberActions } from '../../actions/subscriber.actions';

const Highlight = ({ children }) => (
  <strong className="bg-warning text-dark">{children}</strong>
);

const convertToHighlightedText = (text, query) => (
  <Highlighter
    searchWords={[query]}
    autoEscape={true}
    caseSensitive={true}
    textToHighlight={text}
    highlightTag={Highlight}
  />);

const CustomerRow = ({ customer, select, query }) => (
  <Card>
    <CardBody>
      <CardText>
        {convertToHighlightedText(customer.nickname, query)}<br />
        {convertToHighlightedText(customer.contactEmail, query)}<br />
        {convertToHighlightedText(customer.id, query)}<br />
        <br />
        <Button color="primary" onClick={() => select(customer)}>{'Show details'}</Button>
      </CardText>
    </CardBody>
  </Card>);

CustomerRow.propTypes = {
  customer: PropTypes.shape({
    id: PropTypes.string,
    nickname: PropTypes.string,
    contactEmail: PropTypes.string,
  }),
  query: PropTypes.string,
  select: PropTypes.func.isRequired
};

const CustomerList = props => {
  let query = localStorage.getItem('searchedEmail')

  // If customer is set, remove the list.
  if (props.customer.id || !Array.isArray(props.subscribers)) {
    return null;
  }

  const listItems = props.subscribers.map((customer, index) =>
    <div key={index}>
      <CustomerRow
        customer={customer}
        query={query}
        select={props.selectCustomer}
        key={index} />
      <br />
    </div>
  );
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
  select: PropTypes.func.isRequired
};

function mapStateToProps(state) {
  const { subscribers, customer } = state

  return {
    subscribers,
    customer
  };
}
const mapDispatchToProps = {
  select: subscriberActions.selectCustomer
}
export default connect(mapStateToProps, mapDispatchToProps)(CustomerList);
