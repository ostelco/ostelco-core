import React from 'react';
import _ from 'lodash';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { Card, CardBody } from 'reactstrap';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import ReactTable from "react-table"
import "react-table/react-table.css";

import { convertTimestampToDate } from '../../helpers';

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

const AuditLogs = ({ auditLogs }) => {
  let columns = [
    {
      Header: "Date",
      id: "1",
      accessor: d => convertTimestampToDate(d.timestamp),
      width: 200
    },
    {
      Header: "Severity",
      id: "2",
      accessor: "severity",
      width: 200,
      Cell: row => (
        <div
          style={{
            backgroundColor: row.value === "INFO" ? '#85cc00'
              : row.value === "WARN" ? '#ffbf00'
                : '#ff2e00',
            borderRadius: '2px',
            transition: 'all .2s ease-out'
          }}
        >
          &nbsp;&nbsp;&nbsp;
            <FontAwesomeIcon icon={severityToIcon(row.value)} />
          &nbsp;&nbsp;
            {row.value}
        </div>
      )
    },
    {
      Header: "Message",
      id: "3",
      accessor: "message",
      filterMethod: (filter, row) =>
        _.includes(row[filter.id], filter.value)
    }
  ]

  return (
    <Card>
      <CardBody>
        <ReactTable
          data={auditLogs}
          columns={columns}
          className="-striped -highlight"
          filterable
        />
      </CardBody>
    </Card>
  );
}

AuditLogs.propTypes = {
  auditLogs: PropTypes.array
};

function mapStateToProps(state) {
  let auditLogs = state.auditLogs;
  // While fetching the values, the state properies will be set to { loading: true } object.
  // This case is handled here by passing an empty array.
  return {
    auditLogs: Array.isArray(auditLogs) ? auditLogs : []
  };
}

export default connect(mapStateToProps)(AuditLogs);
