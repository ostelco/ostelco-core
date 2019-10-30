import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { Button, Col, Form, FormGroup, Input, Label, Row } from 'reactstrap';

import { getTextType } from '../../helpers';

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
    if (type === 'phonenumber' || type === 'email') {
      return 'success'
    };
    const length = value.length;
    if (length > 5) {
      return 'warning'
    };
    return null;
  }

  return { value, onChange, onSubmit, onValidateInput };
}

export default function SearchForm(props) {
  let email = localStorage.getItem('searchedEmail')
  const input = useFormInput(email, props.onSubmit)
  return (
    <div className="container">
      <Form onSubmit={input.onSubmit}>
        <FormGroup>
          <br />
          <Label>Search user by contact Email or phone number</Label>
          <Row>
            <Col>
              <Input
                type="text"
                value={input.value}
                onChange={input.onChange}
                placeholder="Enter text"
              />
            </Col>
            <Col xs={"auto"} md={"auto"}>
              <Button color="outline-primary" type="submit">Search</Button>
            </Col>
          </Row>
        </FormGroup>
      </Form>
    </div>
  );
}

SearchForm.propTypes = {
  onSubmit: PropTypes.func.isRequired
};
