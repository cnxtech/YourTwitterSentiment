import { combineReducers } from 'redux';
import SocketKafkaMessageReducer from './socketKafkaMessageReducer'

const rootReducer = combineReducers({
  kafkaMessage: SocketKafkaMessageReducer
})

export default rootReducer;
