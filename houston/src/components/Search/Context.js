import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { Card, CardBody } from 'reactstrap';
import ReactJson from 'react-json-view';

function Context({ context }) {
  return (
    <Card>
      <CardBody>
        <ReactJson
          src={context}
          displayDataTypes={false}
          displayObjectSize={false}
          theme={"bright:inverted"} />
      </CardBody>
    </Card>
  );
}

Context.propTypes = {
  context: PropTypes.object
};

function mapStateToProps(state) {
  const { context } = state;
  return {
    context
  };
}
export default connect(mapStateToProps)(Context);
