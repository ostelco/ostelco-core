import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';

import DataUsage from "./DataUsage";
import Profile from "./Profile";
import PaymentHistory from "./PaymentHistory";

class SearchResults extends React.Component {
  render() {
    return (
      <div className="container">
        <Profile />
        <br />
        <DataUsage />
        <br />
        <PaymentHistory />
      </div>
    );
  }
}

export default SearchResults;
