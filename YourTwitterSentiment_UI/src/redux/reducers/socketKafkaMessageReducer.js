import { SOCKET_KAFKA_MESSAGE } from '../constants'

export default function (state = [], action) {
  switch (action.type) {
    case SOCKET_KAFKA_MESSAGE:
      const payload = action.payload
      if(payload.prop && payload.prop.constructor === Array){
        console.log("ists an array"+ payload)
        return payload
      }
      else{
        let messages = [].push(payload)
        console.log("Not array array "+ messages)

        return messages
      }
    default:
      return state
  }
}
