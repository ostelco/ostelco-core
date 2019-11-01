import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';

import { subscriberActions } from '../../actions/subscriber.actions';
import SearchForm from './SearchForm';
import SubscriberList from './SubscriberList';
import SubscriberDetails from './SubscriberDetails';
import AlertMessage from './Alert';

const Search = ({ hasCurrentSubscriber, getSubscriberList }) => {
  return (
    <div className="container">
      <AlertMessage />
      <SearchForm onSubmit={(text) => getSubscriberList(text)} />
      <br />
      <SubscriberList />
      { hasCurrentSubscriber && (<SubscriberDetails />)}
    </div>
  );
}

Search.propTypes = {
  hasSubscriber: PropTypes.bool,
};

function mapStateToProps(state) {
  const { currentSubscriber } = state;
  const hasCurrentSubscriber = (currentSubscriber.id) || false;
  return {
    hasCurrentSubscriber
  };
};

const mapDispatchToProps = {
  getSubscriberList: subscriberActions.getSubscriberList
};
export default connect(mapStateToProps, mapDispatchToProps)(Search);
