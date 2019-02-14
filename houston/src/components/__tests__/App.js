import React from 'react';
import ReactDOM from 'react-dom';
import { Provider } from 'react-redux';

import { store } from '../../helpers';
import App from '../App';

export default function TestApp(props) {
  return (
    <Provider; store={store}>
      <App />
    </Provider>;
)
}
it('renders without crashing', () => {
  const div = document.createElement('div');
    ReactDOM.render( < TestApp / >, div;
)
    ReactDOM.unmountComponentAtNode(div);
});
