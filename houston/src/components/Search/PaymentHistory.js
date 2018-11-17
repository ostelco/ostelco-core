import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { Table, Card, CardBody, CardTitle, Button, UncontrolledTooltip } from 'reactstrap';

import { subscriberActions } from '../../actions';
import { convertTimestampToDate } from '../../helpers';

const HistoryRow = props => {
  function onRefund(e) {
    e.preventDefault();
    console.log(`Reverting ${props.item.id}`);
    props.refundPurchase(props.item.id, 'requested_by_customer');
  }
  function nope(e) {
    e.preventDefault();
  }
  function renderOption() {
    if (props.item.refund && props.item.refund.id) {
      return (
        <td>
        <Button color="outline-secondary" onClick={nope} id={props.item.refund.id}>Refunded..</Button>
        <UncontrolledTooltip placement="right" target={props.item.refund.id}>
          {`Refunded on ${convertTimestampToDate(props.item.refund.timestamp)}, ${props.item.refund.reason}`}
        </UncontrolledTooltip>
      </td>
      );
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
    id: PropTypes.string,
    product: PropTypes.shape({
      presentation: PropTypes.shape({
        priceLabel: PropTypes.string,
        productLabel: PropTypes.string
      }),
    }),
    timestamp: PropTypes.number
  }),
  refundPurchase: PropTypes.func.isRequired
};

const PaymentHistory = props => {
  if (!Array.isArray(props.paymentHistory)) return null;
  const listItems = props.paymentHistory.map((history, index) =>
    <HistoryRow item={history} key={history.id} refundPurchase={props.refundPurchase}/>
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
  const { loggedIn } = state.authentication;
  const { paymentHistory } = state;

  return {
    loggedIn,
    paymentHistory: paymentHistory.data
  };
}
const mapDispatchToProps = {
  refundPurchase: subscriberActions.refundPurchase
}
export default connect(mapStateToProps, mapDispatchToProps)(PaymentHistory);
