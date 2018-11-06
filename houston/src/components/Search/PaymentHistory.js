import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { authActions, pseudoActions } from '../../actions';
import { Panel, Table } from 'react-bootstrap';

const PaymentHistory = props => {
  return (
    <Panel>
      <Panel.Heading>Payment History</Panel.Heading>
      <Panel.Body>
      <samp>
        <Table striped bordered condensed hover>
          <thead>
            <tr>
              <th>#</th>
              <th>Date</th>
              <th>Plan</th>
              <th>Price</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>1</td>
              <td>15 Oct 2018</td>
              <td>1 GB</td>
              <td>1$</td>
            </tr>
            <tr>
              <td>2</td>
              <td>16 Oct 2018</td>
              <td>1 GB</td>
              <td>1$</td>
            </tr>
            <tr>
              <td>2</td>
              <td>18 Oct 2018</td>
              <td>5 GB</td>
              <td>5$</td>
            </tr>
          </tbody>
        </Table>
        </samp>
      </Panel.Body>
    </Panel>
  );
}

PaymentHistory.propTypes = {
  loggedIn: PropTypes.bool,
  pseudonym: PropTypes.object,
};

function mapStateToProps(state) {
  const { loggedIn } = state.authentication;
  const { pseudonym } = state;

  return {
    loggedIn,
    pseudonym,
  };
}
const mapDispatchToProps = {
  login: authActions.login,
  getPseudonym: pseudoActions.getPseudonym
}
export default connect(mapStateToProps, mapDispatchToProps)(PaymentHistory);
