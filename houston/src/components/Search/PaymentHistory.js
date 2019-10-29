import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { Table, Card, CardBody, CardHeader, Button, UncontrolledTooltip } from 'reactstrap';

import { subscriberActions } from '../../actions/subscriber.actions';
import { convertTimestampToDate } from '../../helpers';
import WarningModal from '../Shared/WarningModal';


export const RefundedItemOption = (props) => {
  function nope(e) {
    e.preventDefault();
  }
  return (
    <td>
      <Button color="outline-secondary" onClick={nope} id={props.id}>Refunded..</Button>
      <UncontrolledTooltip placement="right" target={props.id}>
        {`Refunded on ${convertTimestampToDate(props.timestamp)}, ${props.reason}`}
      </UncontrolledTooltip>
    </td>
  );
}

export const FreeItemOption = props => (<td />);
export const HistoryRow = props => {
  const isRefunded = () => (props.item.refund && props.item.refund.id);
  const isFreeProduct = () => (props.item.product.price.amount <= 0);
  function onRefund(e) {
    e.preventDefault();
    console.log(`Reverting ${props.item.id}`);
    props.refundPurchase(props.item.id, 'requested_by_customer');
  }

  function renderOption() {
    if (isRefunded()) {
      return (<RefundedItemOption {...props.item.refund} />);
    } else if (isFreeProduct()) {
      return (<FreeItemOption />);
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
      reason: PropTypes.string,
      timestamp: PropTypes.number.isRequired
    }),
    timestamp: PropTypes.number.isRequired
  }),
  refundPurchase: PropTypes.func.isRequired
};

class PaymentHistory extends React.Component {
  constructor(props, context) {
    super(props, context);
    this.state = {
      showConfirm: false
    };
  }
  handleCloseConfirm = () => {
    const state = this.state;
    state.showConfirm = false;
    this.setState(state);
  }

  handleShowConfirm = (id, reason) => {
    const state = { ...this.state, id, reason };
    state.showConfirm = true;
    this.setState(state);
  }

  handleConfirm = () => {
    this.handleCloseConfirm();
    const { id, reason } = this.state;
    // TODO call the method to give additional data
    console.log(`User confirmed, refunding id:${id}, reason:${reason}`);
    this.props.refundPurchase(this.state.id, this.state.reason);
  }

  render() {
    const { props } = this;
    if (!props.paymentHistory) {
      return null;
    }
    const refundHeading = 'Confirm refund operartion';
    const refundText = 'Do you really want to refund this transaction ?';
    const listItems = props.paymentHistory.map((history) =>
      <HistoryRow item={history} key={history.id} refundPurchase={this.handleShowConfirm} />
    );

    return (
      <Card>
        <CardHeader>Payment History</CardHeader>
        <CardBody>
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
        <WarningModal
          heading={refundHeading}
          warningText={refundText}
          show={this.state.showConfirm}
          handleConfirm={this.handleConfirm}
          handleClose={this.handleCloseConfirm} />
      </Card>
    );
  }
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
