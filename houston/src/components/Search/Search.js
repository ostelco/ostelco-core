import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';

import { subscriberActions } from '../../actions/subscriber.actions';
import SearchForm from './SearchForm';
import SubscriberList from './SubscriberList';
import SubscriberDetails from './SubscriberDetails';
import AlertMessage from './Alert';

const Search = ({ currentSubscriber, getSubscriberList }) => {
  const hasSubscriber = (currentSubscriber.id) || false;
  return (
    <div className="container">
      <AlertMessage />
      <SearchForm onSubmit={(text) => getSubscriberList(text)} />
      <br />
      <SubscriberList />
      { hasSubscriber && (<SubscriberDetails />)}
    </div>
  );
}

Search.propTypes = {
  loggedIn: PropTypes.bool,
  currentSubscriber: PropTypes.object,
};

function mapStateToProps(state) {
  const { authentication: { loggedIn }, currentSubscriber } = state;
  return {
    loggedIn,
    currentSubscriber
  };
};

const mapDispatchToProps = {
  getSubscriberList: subscriberActions.getSubscriberList
};
export default connect(mapStateToProps, mapDispatchToProps)(Search);
