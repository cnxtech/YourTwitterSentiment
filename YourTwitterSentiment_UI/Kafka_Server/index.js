const io = require('socket.io')();

const kafka = require('kafka-node');
const Consumer = kafka.Consumer;
const Offset = kafka.Offset;
const Client = kafka.Client;
const tweetSentimenttopic = 'yourtwittersentiment'
const tagsSentimenttopic = 'tagsentiment'

const client = new Client('localhost:2181');
const topics = [
    {topic: tweetSentimenttopic, partition: 0},
    {topic: tagsSentimenttopic, partition: 0}
];
const options = { autoCommit: false, fetchMaxWaitMs: 1000, fetchMaxBytes: 1024 * 1024 };

const consumer = new Consumer(client, topics, options);
const offset = new Offset(client);

consumer.on('message', function (message) {
  socketMessage(io, message)
});

consumer.on('error', function (err) {
  console.log('error', err);
});

function socketMessage(io, message){
  console.log("-------------------New Message----------------------------------------------")
  console.log(message)
  console.log("-------------------New Message End----------------------------------------------")
  if(message.topic === tweetSentimenttopic){
    io.sockets.emit('tweetsMessage', message);
  }else{
    io.sockets.emit('tagsMessage', message);
  }

}

const port = 8000;
io.listen(port);
console.log('listening on port ', port);
