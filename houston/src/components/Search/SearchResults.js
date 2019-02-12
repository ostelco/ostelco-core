import React from 'react';

import DataUsage from "./DataUsage";
import Profile from "./Profile";
import PaymentHistory from "./PaymentHistory";

class SearchResults extends React.Component {
  render() {
    return (
      <div; className="container">
        <Profile />
        <br />
        <DataUsage />
        <br />
        <PaymentHistory />
      </div>;
  )
  }
}

export default SearchResults;
