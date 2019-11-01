import React from 'react';
import { useDispatch, useSelector } from 'react-redux';

import { subscriberActions } from '../../actions/subscriber.actions';
import SearchForm from './SearchForm';
import SubscriberList from './SubscriberList';
import SubscriberDetails from './SubscriberDetails';
import AlertMessage from './Alert';

const Main = () => {
  const dispatch = useDispatch();
  const currentSubscriber = useSelector(state => state.currentSubscriber)
  const hasCurrentSubscriber = (currentSubscriber.id) || false;

  return (
    <div className="container">
      <AlertMessage />
      <SearchForm onSubmit={(text) => dispatch(subscriberActions.getSubscriberList(text))} />
      <br />
      <SubscriberList />
      { hasCurrentSubscriber && (<SubscriberDetails />)}
    </div>
  );
}

export default Main;
