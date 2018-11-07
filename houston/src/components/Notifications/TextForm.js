import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { Form,  Button, FormGroup, ControlLabel, FormControl } from 'react-bootstrap';

export class TextForm extends React.Component {
  constructor(props, context) {
    super(props, context);
    this.handleChange = this.handleChange.bind(this);
    this.state = {
      value: ''
    };
  }

  handleChange(e) {
    this.setState({ value: e.target.value });
  }

  onSubmit = (e) => {
    e.preventDefault();
    //handle form processing here....
    console.log("SearchForm On Submit")
    this.props.onSubmit(this.state.value)
  }

  render() {
    return (
        <Form onSubmit={this.onSubmit}>
          <FormGroup
            controlId="formBasicText"
          >
            <ControlLabel></ControlLabel>
            <FormControl
              componentClass="textarea"
              value={this.state.value}
              placeholder="Enter text"
              onChange={this.handleChange}
            />
            </FormGroup>
             <Button bsStyle="primary" type="submit">Send notification</Button>
        </Form>
    );
  }
}

TextForm.propTypes = {
  onSubmit: PropTypes.func.isRequired
};

export default TextForm;
