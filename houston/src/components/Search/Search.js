import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { authActions, subscriberActions } from '../../actions';
import { SearchForm } from './SearchForm';
import SearchResults from './SearchResults';

class Search extends React.Component {

  onSubmit = (text) => {
    //handle form processing here....
    console.log("Search On Submit")
    this.props.getSubscriberAndBundles(text)
  }

  render() {
    const hasResults = this.props.profile.name || false;

    return (
      <div className="container">
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
  login: authActions.login,
  getSubscriberAndBundles: subscriberActions.mockGetSubscriberAndBundles
}
export default connect(mapStateToProps, mapDispatchToProps)(Search);
