import React from 'react';
import { bindActionCreators } from 'redux'
import { useDispatch, useSelector } from 'react-redux';
import { Button, Form, FormGroup, Label, Input } from 'reactstrap';

import { notifyActions } from '../../actions/notifiy.actions';
// This uses redux hooks
// https://react-redux.js.org/api/hooks

function NotificationEditor({ messageLabel, submitLabel, titleLabel }) {
  const dispatch = useDispatch();
  const actions = bindActionCreators(notifyActions, dispatch);

  const notification = useSelector(state => state.notification);
  const { message, title } = notification;

  function onSubmit(e) {
    e.preventDefault();
    actions.sendNotificationToSubscriber(title, message);
  }
  return (
    <Form onSubmit={onSubmit}>
      <FormGroup>
        <Label for="inputTitle">{titleLabel}</Label>
        <Input
          name="text"
          id="inputTitle"
          value={title}
          onChange={(e) => actions.setNotificationTitle(e.target.value)}
          placeholder="Enter title"
        />
      </FormGroup>
      <FormGroup>
        <Label for="inputMessage">{messageLabel}</Label>
        <Input
          type="textarea"
          name="text"
          id="inputMessage"
          value={message}
          onChange={(e) => actions.setNotificationMessage(e.target.value)}
          placeholder="Enter message"
        />
      </FormGroup>
      <Button bsstyle="primary" type="submit">{submitLabel}</Button>
    </Form>
  );
}

export default NotificationEditor;
