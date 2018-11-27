import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { Button, Form, FormGroup, Label, Input } from 'reactstrap';

import { getTextType } from '../../helpers';

export default function SearchForm(props) {
  const input = useFormInput('martin.cederlof@telenordigital.com', props.onSubmit)
  return (
    <div className="container">
      <Form onSubmit={input.onSubmit}>
        <FormGroup>
          <br />
          <Label>Search user by phone number or email</Label>
          <Input
            type="email"
            value={input.value}
            onChange={input.onChange}
            placeholder="Enter text"
          />
        </FormGroup>
        <Button color="outline-primary" type="submit">Search</Button>
      </Form>
    </div>
  );
}

SearchForm.propTypes = {
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

  function onValidateInput() {
    const type = getTextType(value);
    if (type === 'phonenumber' || type === 'email') return 'success';
    const length = value.length;
    if (length > 5) return 'warning';
    return null;
  }

  return { value, onChange, onSubmit, onValidateInput };
}
