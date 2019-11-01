import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';

import { subscriberActions } from '../../actions/subscriber.actions';
import SearchForm from './SearchForm';
import CustomerList from './CustomerList';
import SearchResults from './SearchResults';
import AlertMessage from './Alert';

class Search extends React.Component {

  onSubmit = (text) => {
    this.props.getSubscriberList(text);
  }

  render() {
    const hasResults = (this.props.currentSubscriber.id) || false;
    return (
      <div className="container">
        <AlertMessage />
        <SearchForm onSubmit={this.onSubmit} />
        <br />
        <CustomerList />
        {
          hasResults && (
            <SearchResults />
          )
        }
      </div>
    );
  }
}

Search.propTypes = {
  loggedIn: PropTypes.bool,
  pseudonym: PropTypes.object,
  currentSubscriber: PropTypes.object,
};

function mapStateToProps(state) {
  const { loggedIn } = state.authentication;
  const { currentSubscriber } = state;
  return {
    loggedIn,
    currentSubscriber
  };
};

const mapDispatchToProps = {
  getSubscriberList: subscriberActions.getSubscriberList
};
export default connect(mapStateToProps, mapDispatchToProps)(Search);
