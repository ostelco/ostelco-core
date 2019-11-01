import React, { useState } from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { Button, Col, Row, Card, CardBody, CardHeader } from 'reactstrap';
import _ from 'lodash';

import WarningModal from '../Shared/WarningModal';
import { humanReadableBytes } from '../../helpers';

const DataUsage = ({ balance }) => {
  const [showWarning, setShowWarning] = useState(false);

  function handleConfirmModal() {
    setShowWarning(false);
    // TODO call the method to give additional data
  }

  const modalHeading = `Confirm additional Data`
  const modalText = `Do you really want to give this user an additional 1 GB of Data ?`
  // don't show the component when balance string is empty
  if (!balance) return null;

  return (
    <Card>
      <CardHeader>Data balance</CardHeader>
      <CardBody>
        <Row >
          <Col xs={6} md={4}>
            {`Remaining ${balance}.`}
          </Col>
          <Col xs={6} md={4}>
            <Button disabled={true} onClick={() => setShowWarning(true)}>{'Give additional 1 GB'}</Button>
          </Col>
        </Row>
        <WarningModal
          heading={modalHeading}
          warningText={modalText}
          show={showWarning}
          handleConfirm={handleConfirmModal}
          handleClose={() => setShowWarning(false)} />
      </CardBody>
    </Card>
  );
}

DataUsage.propTypes = {
  balance: PropTypes.string
};

function mapStateToProps(state) {
  const firstBalance = _.get(state, 'bundles[0].balance')
  return {
    balance: firstBalance ? humanReadableBytes(firstBalance) : null
  };
}

export default connect(mapStateToProps)(DataUsage);
