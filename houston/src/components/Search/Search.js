import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import _ from 'lodash';

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
    const hasResults = (this.props.profile && this.props.profile.nickname) || false;
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
  profile: PropTypes.object,
};

function mapStateToProps(state) {
  const { loggedIn } = state.authentication;
  const subscriber = _.get(state, 'customer')
  return {
    loggedIn,
    profile: subscriber
  };
};

const mapDispatchToProps = {
  getSubscriberList: subscriberActions.getSubscriberList
};
export default connect(mapStateToProps, mapDispatchToProps)(Search);
