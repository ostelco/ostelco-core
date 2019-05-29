import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { Card, CardBody } from 'reactstrap';
import ReactJson from 'react-json-view';

class Context extends React.Component {

  render() {
    const { props } = this;
    if (!props.context) {
      return null;
    }

    return (
      <Card>
        <CardBody>
          <ReactJson
            src={props.context}
            displayDataTypes={false}
            displayObjectSize={false}
            theme={"bright:inverted"} />
        </CardBody>
      </Card>
    );
  }
}

Context.propTypes = {
  context: PropTypes.object
};

function mapStateToProps(state) {
  let context = state.context;
  console.log(JSON.stringify(state, null, 4));
  return {
    context
  };
}
export default connect(mapStateToProps)(Context);
