import React, { Component } from 'react';
import loading from './loading.svg';
import { Redirect } from 'react-router-dom';
import { connect } from 'react-redux';

class Callback extends Component {
  render() {
    if (this.props.loggedIn) {
      return <Redirect to="/" />;
    }

    const style = {
      position: 'absolute',
      display: 'flex',
      justifyContent: 'center',
      height: '100vh',
      width: '100vw',
      top: 0,
      bottom: 0,
      left: 0,
      right: 0,
      backgroundColor: 'white',
    };

    return (
      <div style={style}>
        <img src={loading} alt="loading" />
      </div>
    );
  }
}

function mapStateToProps(state) {
  const { loggedIn } = state.authentication;
  return {
    loggedIn
  };
};

export default connect(mapStateToProps)(Callback);
