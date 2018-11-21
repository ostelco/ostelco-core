import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { Table, Card, CardBody, CardTitle, Button, UncontrolledTooltip } from 'reactstrap';

import { subscriberActions } from '../../actions/subscriber.actions';
import { convertTimestampToDate } from '../../helpers';

export const HistoryRow = props => {
  const isRefunded = () => (props.item.refund && props.item.refund.id);
  const isFreeProduct = () => (props.item.product.price.amount <= 0);
  function onRefund(e) {
    e.preventDefault();
    console.log(`Reverting ${props.item.id}`);
    props.refundPurchase(props.item.id, 'requested_by_customer');
  }
  function nope(e) {
    e.preventDefault();
  }
  function renderOption() {
    if (isRefunded()) {
      return (
        <td>
          <Button color="outline-secondary" onClick={nope} id={props.item.refund.id}>Refunded..</Button>
          <UncontrolledTooltip placement="right" target={props.item.refund.id}>
            {`Refunded on ${convertTimestampToDate(props.item.refund.timestamp)}, ${props.item.refund.reason}`}
          </UncontrolledTooltip>
        </td>
      );
    } else if(isFreeProduct()) {
      return (<td />);
    } else {
      return (
        <td><Button color="outline-primary" onClick={onRefund}>Refund</Button></td>
      );
    }
  }
  return (
    <tr >
      <td>{props.item.product.presentation.productLabel}</td>
      <td>{props.item.product.presentation.priceLabel}</td>
      <td>{convertTimestampToDate(props.item.timestamp)}</td>
      {renderOption()}
    </tr>);
}

HistoryRow.propTypes = {
  item: PropTypes.shape({
    id: PropTypes.string.isRequired,
    product: PropTypes.shape({
      price: PropTypes.shape({
        amount: PropTypes.number.isRequired
      }).isRequired,
      presentation: PropTypes.shape({
        priceLabel: PropTypes.string,
        productLabel: PropTypes.string
      }).isRequired,
    }),
    refund: PropTypes.shape({
      id: PropTypes.string.isRequired,
      timestamp: PropTypes.number.isRequired
    }),
    timestamp: PropTypes.number.isRequired
  }),
  refundPurchase: PropTypes.func.isRequired
};

const PaymentHistory = props => {
  if (!props.paymentHistory) return null;
  const listItems = props.paymentHistory.map((history, index) =>
    <HistoryRow item={history} key={history.id} refundPurchase={props.refundPurchase} />
  );
  return (
    <Card>
      <CardBody>
        <CardTitle>Payment History</CardTitle>
        <Table striped bordered>
          <thead>
            <tr>
              <th>Plan</th>
              <th>Price</th>
              <th>Date</th>
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
  paymentHistory: PropTypes.array,
  refundPurchase: PropTypes.func.isRequired
};

function mapStateToProps(state) {
  let paymentHistory = state.paymentHistory;
  // Pass only arrays
  if (!Array.isArray(paymentHistory)) {
    paymentHistory = null;
  }
  return {
    paymentHistory
  };
}
const mapDispatchToProps = {
  refundPurchase: subscriberActions.refundPurchase
}
export default connect(mapStateToProps, mapDispatchToProps)(PaymentHistory);
