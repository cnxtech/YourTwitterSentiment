// index.js
import React from 'react';
import ReactDOM from 'react-dom';
import { browserHistory } from 'react-router'
import { Provider } from 'react-redux';
import { createStore, applyMiddleware } from 'redux';

const createStoreWithMiddleware = applyMiddleware()(createStore);

import Routes from './routes';
import reducers from './redux/reducers';

import './index.css';
import './html-hint.css'

ReactDOM.render(
  <Provider store={createStoreWithMiddleware(reducers)}>
    <Routes history={browserHistory} />
  </Provider>,
  document.getElementById('root')
);
