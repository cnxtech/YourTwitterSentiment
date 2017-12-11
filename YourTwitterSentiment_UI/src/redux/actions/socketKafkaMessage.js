import {SOCKET_KAFKA_MESSAGE, SUBSCRIBE_SOCKET_KAFKA_MESSAGE } from '../constants'

export const socketKafkaMessage = (message) => {
  return {
    type: SOCKET_KAFKA_MESSAGE,
    payload: message
  }
}

export const subscribeSocketKafkaMessage = (socket) => {
  return {
    type: SUBSCRIBE_SOCKET_KAFKA_MESSAGE,
    payload: socket
  }
}
