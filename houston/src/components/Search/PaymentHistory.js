import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { Table, Card, CardBody, CardTitle, Button } from 'reactstrap';

import { authActions, pseudoActions } from '../../actions';
import { convertTimestampToDate } from '../../helpers';

const HistoryRow = props => {
  return (
    <tr >
      <td>{props.index}</td>
      <td>{convertTimestampToDate(props.item.timestamp)}</td>
      <td>{props.item.product.presentation.productLabel}</td>
      <td>{props.item.product.presentation.priceLabel}</td>
      <td><Button color="link">Revert</Button></td>
    </tr>);
}

HistoryRow.propTypes = {
  index: PropTypes.number.isRequired,
  item: PropTypes.shape({
    id: PropTypes.string,
    product: PropTypes.shape({
      presentation: PropTypes.shape({
        priceLabel: PropTypes.string,
        productLabel: PropTypes.string
      }),
    }),
    timestamp: PropTypes.number
  })
};

const PaymentHistory = props => {
  if (!Array.isArray(props.paymentHistory)) return null;
  const listItems = props.paymentHistory.map((history, index) =>
    <HistoryRow item={history} index={index + 1} key={history.id} />
  );
  return (
    <Card>
      <CardBody>
        <CardTitle>Payment History</CardTitle>
          <Table striped bordered>
            <thead>
              <tr>
                <th>#</th>
                <th>Date</th>
                <th>Plan</th>
                <th>Price</th>
                <th>Options</th>
              </tr>
            </thead>
            <tbody>
              {listItems}
            </tbody>
          </Table>
      </CardBody>
    </Card>
  );
}

PaymentHistory.propTypes = {
  loggedIn: PropTypes.bool,
  pseudonym: PropTypes.object,
  paymentHistory: PropTypes.array,
};

function mapStateToProps(state) {
  const { loggedIn } = state.authentication;
  const { pseudonym } = state;
  const { paymentHistory } = state;

  return {
    loggedIn,
    pseudonym,
    paymentHistory: paymentHistory.data
  };
}
const mapDispatchToProps = {
  login: authActions.login,
  getPseudonym: pseudoActions.getPseudonym
}
export default connect(mapStateToProps, mapDispatchToProps)(PaymentHistory);
