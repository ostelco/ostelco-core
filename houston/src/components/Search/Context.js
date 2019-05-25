import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { Card, CardBody, CardTitle } from 'reactstrap';

class Context extends React.Component {

  render() {
    const { props } = this;
    if (!props.context) {
      return null;
    }

    return (
      <Card>
        <CardBody>
          <CardTitle>Context</CardTitle>
          <pre className="language-jsx">
            {JSON.stringify(props.context, null, 4)}
          </pre>
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
