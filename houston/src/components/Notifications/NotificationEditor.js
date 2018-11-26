import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { Button, Form, FormGroup, Label, Input } from 'reactstrap';
import _ from 'lodash';

import { notifyActions } from '../../actions/notifiy.actions';

function NotificationEditor(props) {
  function onSubmit(e) {
    e.preventDefault();
    props.onSubmit(props.title, props.message);
  }
  const { email } = props;
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
      {
        email && (
          <FormGroup check>
            <Label check>
              <Input
                type="checkbox" 
                checked={props.type}
                onChange={(e) => props.setNotificationType(e.target.checked)}
              />{' '}
              Send notification only to {email}
            </Label>
            <hr/>
          </FormGroup>
      )}
      <Button bsstyle="primary" type="submit">{props.submitLabel}</Button>
    </Form>
  );
}

NotificationEditor.propTypes = {
  titleLabel: PropTypes.string.isRequired,
  messageLabel: PropTypes.string.isRequired,
  submitLabel: PropTypes.string.isRequired,
  onSubmit: PropTypes.func.isRequired
};

function mapStateToProps(state) {
  let notification = state.notification;
  const email = _.get(state, 'subscriber.email');
  console.log(state);
  return {
    message: notification.message,
    title: notification.title,
    type: notification.type,
    email
  };
}
const mapDispatchToProps = {
  setNotificationMessage: notifyActions.setNotificationMessage,
  setNotificationTitle: notifyActions.setNotificationTitle,
  setNotificationType: notifyActions.setNotificationType
}
export default connect(mapStateToProps, mapDispatchToProps)(NotificationEditor);
