import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { Button, Form, FormGroup, Label, Input } from 'reactstrap';

export default function TextForm(props) {
  const input = useFormInput('', props.onSubmit);
  return (
    <Form onSubmit={input.onSubmit}>
      <FormGroup>
        <Label for="inputText">{props.inputLabel}</Label>
        <Input
          type="textarea"
          name="text"
          id="inputText"
          value={input.value}
          onChange={input.onChange}
          placeholder="Enter text"
        />
      </FormGroup>
      <Button bsstyle="primary" type="submit">{props.submitLabel}</Button>
    </Form>
  );
}

TextForm.propTypes = {
  inputLabel: PropTypes.string.isRequired,
  submitLabel: PropTypes.string.isRequired,
  onSubmit: PropTypes.func.isRequired
};

function useFormInput(initialValue, submit) {
  const [value, setValue] = useState(initialValue);

  function onChange(e) {
    setValue(e.target.value);
  }

  function onSubmit(e) {
    e.preventDefault();
    submit(value);
  }

  return { value, onChange, onSubmit };
}