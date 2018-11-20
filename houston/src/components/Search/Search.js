import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';

import { subscriberActions } from '../../actions/subscriber.actions';
import SearchForm from './SearchForm';
import SearchResults from './SearchResults';
import AlertMessage from './Alert';

class Search extends React.Component {

  onSubmit = (text) => {
    //handle form processing here....
    console.log("Search On Submit")
    this.props.getSubscriberAndBundlesByEmail(text)
  }

  render() {
    const hasResults = this.props.profile.name || false;

    return (
      <div className="container">
        <AlertMessage />
        <SearchForm onSubmit={this.onSubmit} />
        <br />
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
  const { subscriber } = state;
  return {
    profile: subscriber
  };
}
const mapDispatchToProps = {
  getSubscriberAndBundlesByEmail: subscriberActions.getSubscriberAndBundlesByEmail
}
export default connect(mapStateToProps, mapDispatchToProps)(Search);
