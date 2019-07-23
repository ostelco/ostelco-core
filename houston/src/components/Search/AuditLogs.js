import React from 'react';
import _ from 'lodash';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { Table, Card, CardBody, CardTitle } from 'reactstrap';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'

import { convertTimestampToDate } from '../../helpers';

function severityToTableRowClass(severity) {
  switch (_.toLower(severity)) {
    case "free":
    case "warn":
    case "warning":
      return "table-warning";
    case "$5":
    case "error":
      return "table-danger";
    default:
      return null; // Default table class
  }
}
function severityToIcon(severity) {
  switch (_.toLower(severity)) {
    case "free":
    case "warn":
    case "warning":
      return "exclamation-triangle";
    case "$5":
    case "error":
      return "bomb";
    default:
      return "info"
  }
}
export const AuditLogRow = props => {
  return (
    <tr className={severityToTableRowClass(props.item.product.presentation.priceLabel)}>
      <th><FontAwesomeIcon icon={severityToIcon(props.item.product.presentation.priceLabel)} /></th>
      <td>{convertTimestampToDate(props.item.timestamp)}</td>
      <td>{props.item.product.presentation.priceLabel}</td>
      <td>{props.item.product.presentation.productLabel}</td>
    </tr>);
}

AuditLogRow.propTypes = {
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
  })
};

class AuditLogs extends React.Component {
  constructor(props, context) {
    super(props, context);
    this.state = {
      showConfirm: false
    };
  }


  render() {
    const { props } = this;
    if (!props.auditLogs) {
      return null;
    }
    const listItems = props.auditLogs.map((log) =>
      <AuditLogRow item={log} key={log.id} />
    );

    return (
      <Card>
        <CardBody>
          <CardTitle>Audit Logs</CardTitle>
          <Table bordered>
            <thead>
              <tr>
                <th></th>
                <th>Date</th>
                <th>Severity</th>
                <th>Message</th>
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
}

AuditLogs.propTypes = {
  loggedIn: PropTypes.bool,
  auditLogs: PropTypes.array
};

function mapStateToProps(state) {
  let auditLogs = state.auditLogs;
  // Pass only arrays
  if (!Array.isArray(auditLogs)) {
    auditLogs = null;
  }
  return {
    auditLogs
  };
}

export default connect(mapStateToProps)(AuditLogs);
