import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { Button, Form, FormGroup, Label, Input } from 'reactstrap';

import { notifyActions } from '../../actions/notifiy.actions';

function NotificationEditor(props) {
  function onSubmit(e) {
    e.preventDefault();
    props.sendNotificationToSubscriber(props.title, props.message);
  }
  return (
    <Form onSubmit={onSubmit}>
      <FormGroup>
        <Label for="inputTitle">{props.titleLabel}</Label>
        <Input
          name="text"
          id="inputTitle"
          value={props.title}
          onChange={(e) => props.setNotificationTitle(e.target.value)}
          placeholder="Enter title"
        />
      </FormGroup>
      <FormGroup>
        <Label for="inputMessage">{props.messageLabel}</Label>
        <Input
          type="textarea"
          name="text"
          id="inputMessage"
          value={props.message}
          onChange={(e) => props.setNotificationMessage(e.target.value)}
          placeholder="Enter message"
        />
      </FormGroup>
      <Button bsstyle="primary" type="submit">{props.submitLabel}</Button>
    </Form>
  );
}

NotificationEditor.propTypes = {
  titleLabel: PropTypes.string.isRequired,
  messageLabel: PropTypes.string.isRequired,
  submitLabel: PropTypes.string.isRequired,
};

function mapStateToProps(state) {
  let notification = state.notification;
  return {
    message: notification.message,
    title: notification.title,
    type: notification.type
  };
}
const mapDispatchToProps = {
  setNotificationMessage: notifyActions.setNotificationMessage,
  setNotificationTitle: notifyActions.setNotificationTitle,
  setNotificationType: notifyActions.setNotificationType,
  sendNotificationToSubscriber: notifyActions.sendNotificationToSubscriber
}
export default connect(mapStateToProps, mapDispatchToProps)(NotificationEditor);
