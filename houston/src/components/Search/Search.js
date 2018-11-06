import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { authActions, pseudoActions } from '../../actions';
import { SearchForm } from './SearchForm';
import SearchResults from './SearchResults';

class Search extends React.Component {

  onSubmit = () => {
    //handle form processing here....
    console.log("Search On Submit")
  }

  render() {
    const hasResults = this.props.results || false;

    return (
      <div className="container">
        <SearchForm onSubmit = {this.onSubmit} />
        {
          hasResults && (
            <div className="container">
              <br></br>
              <SearchResults />
            </div>
          )
        }
      </div>
    );
  }
}

Search.propTypes = {
  loggedIn: PropTypes.bool,
  pseudonym: PropTypes.object,
  results: PropTypes.array,
};

function mapStateToProps(state) {
  const { loggedIn } = state.authentication;
  const { pseudonym } = state;
  return {
    loggedIn,
    pseudonym,
    results: ["a user is available"]
  };
}
const mapDispatchToProps = {
  login: authActions.login,
  getPseudonym: pseudoActions.getPseudonym
}
export default connect(mapStateToProps, mapDispatchToProps)(Search);
