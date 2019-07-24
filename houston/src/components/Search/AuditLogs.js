import React from 'react';
import _ from 'lodash';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { Table, Card, CardBody, CardTitle } from 'reactstrap';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'

import { convertTimestampToDate } from '../../helpers';

function severityToTableRowClass(severity) {
  switch (_.toLower(severity)) {
    case "warn":
      return "table-warning";
    case "error":
      return "table-danger";
    default:
      return null; // Default table class
  }
}
function severityToIcon(severity) {
  switch (_.toLower(severity)) {
    case "warn":
      return "exclamation-triangle";
    case "error":
      return "bomb";
    default:
      return "info"
  }
}
export const AuditLogRow = props => {
  return (
    <tr className={severityToTableRowClass(props.item.severity)}>
      <th><FontAwesomeIcon icon={severityToIcon(props.item.severity)} />&nbsp; {props.item.severity}</th>
      <td>{convertTimestampToDate(props.item.timestamp)}</td>
      <td>{props.item.message}</td>
    </tr>);
}

AuditLogRow.propTypes = {
  item: PropTypes.shape({
    timestamp: PropTypes.number.isRequired,
    severity: PropTypes.string.isRequired,
    message: PropTypes.string.isRequired
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
      <AuditLogRow item={log} key={log.timestamp} />
    );

    return (
      <Card>
        <CardBody>
          <CardTitle>Audit Logs</CardTitle>
          <Table bordered>
            <thead>
              <tr>
                <th><FontAwesomeIcon icon="bell" />&nbsp;Severity</th>
                <th>Date</th>
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
