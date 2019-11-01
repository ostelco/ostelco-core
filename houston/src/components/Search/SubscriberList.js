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

const SubscriberRow = ({ subscriber, select, query }) => (
  <Card>
    <CardBody>
      <CardText>
        {convertToHighlightedText(subscriber.nickname, query)}<br />
        {convertToHighlightedText(subscriber.contactEmail, query)}<br />
        {convertToHighlightedText(subscriber.id, query)}<br />
        <br />
        <Button color="primary" onClick={() => select(subscriber)}>
          {'Show details'}
        </Button>
      </CardText>
    </CardBody>
  </Card>);

SubscriberRow.propTypes = {
  subscriber: PropTypes.shape({
    id: PropTypes.string,
    nickname: PropTypes.string,
    contactEmail: PropTypes.string,
  }),
  query: PropTypes.string,
  select: PropTypes.func.isRequired
};

const SubscriberList = props => {
  let query = localStorage.getItem('searchedEmail');
  // If subscriber is set, remove the list.
  if (props.currentSubscriber.id || !Array.isArray(props.subscribers)) {
    return null;
  }

  const listItems = props.subscribers.map((subscriber, index) =>
    <div key={index}>
      <SubscriberRow
        subscriber={subscriber}
        query={query}
        select={props.select}
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

SubscriberList.propTypes = {
  subscribers: PropTypes.oneOfType([
    PropTypes.object,
    PropTypes.array
  ]),
  currentSubscriber: PropTypes.object,
  select: PropTypes.func.isRequired
};

function mapStateToProps(state) {
  const { subscribers, currentSubscriber } = state;
  return {
    subscribers,
    currentSubscriber
  };
}
const mapDispatchToProps = {
  select: subscriberActions.selectCurrentSubscriber
};
export default connect(mapStateToProps, mapDispatchToProps)(SubscriberList);
