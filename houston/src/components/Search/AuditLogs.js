import React from 'react';
import _ from 'lodash';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { Card, CardBody, CardTitle } from 'reactstrap';
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

class AuditLogs extends React.Component {
  constructor(props, context) {
    super(props, context);
    this.state = {
      showConfirm: false
    };
  }


  render() {
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

    const { props } = this;
    if (!props.auditLogs) {
      return null;
    }

    return (
      <Card>
        <CardBody>
          <ReactTable
            data={props.auditLogs}
            columns={columns}
            className="-striped -highlight"
            filterable
          />
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
