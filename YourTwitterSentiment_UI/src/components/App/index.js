import React, { Component } from 'react';
import classnames from 'classnames';
import logo from './logo.svg';
import './style.css';
import { connect } from 'react-redux'
import { bindActionCreators } from 'redux'
import { socketKafkaMessage } from '../../redux/actions'
import TwitterMap from '../TwitterMap'
import TwitterMapCustom from '../TwitterMapCustom'
import TwitterTagCloud from '../TagCloud'
import socketIOClient from "socket.io-client";
import _ from 'lodash'

const socket = socketIOClient('http://127.0.0.1:8000');

class App extends Component {
  static propTypes = {}
  static defaultProps = {}

  constructor(props){
    super(props)
    this.state = {
      isFiltered: false,
      messages: [],
      filteredMessages: [],
      tagsMessages: []
    }
    this.updateMessages = this.updateMessages.bind(this)
    this.updateTagsMessages = this.updateTagsMessages.bind(this)
    this.onTagFilter = this.onTagFilter.bind(this)
    this.clearFilter = this.clearFilter.bind(this)
  }

 componentDidMount() {
   socket.on('tweetsMessage', (newkafkaMessage) => this.updateMessages(newkafkaMessage));
   socket.on('tagsMessage', (newkafkaMessage) => this.updateTagsMessages(newkafkaMessage));
  }

  render() {
    const { className } = this.props;
    const messages = this.state.kafkaMessage

    return (
      <div className={classnames('App', className)} >
        <div className="App-header">
          <div>
            <img src={logo} className="App-logo" alt="logo" />
            <h4 style={{display: 'inline', float:'left', padding: '5px', fontWeight: 'bolder', color:'floralwhite'}}>Your Twitter Sentiment</h4>
            <span style={{fontSize: '1em', float: 'left', paddingLeft: '300px', color:'floralwhite'}}> Top 20 tags</span>
          </div>
          <br/>
          {this.state.isFiltered ?
            (<button style={{float: 'right'}} className = "btn btn-primary" onClick = {this.clearFilter}>Clear</button>) :
            (null)
          }
          <TwitterTagCloud data= {this.state.tagsMessages} onTagFilter={this.onTagFilter}/>
        </div>
        <div className="App-intro">
          <div style={{backgroundColor:'black', fontWeight: 'bold', color:'floralwhite', }}><strong>Tweets in the last 30 seconds</strong></div>
          <TwitterMap data={this.state.filteredMessages} />
        </div>
      </div>
    );
  }

  updateMessages(newkafkaMessage){
    const parsedMessages = JSON.parse(newkafkaMessage.value).map(message => JSON.parse(message))
    this.setState({messages: parsedMessages, filteredMessages: parsedMessages, isFiltered: false})
  }

  updateTagsMessages(newkafkaMessage){
    const tagsMessages = JSON.parse(newkafkaMessage.value)
    //const top20Messages = _.take(_.orderBy(tagsMessages, [ 'count'], ['desc']), 20)
    this.setState({tagsMessages})
  }

  onTagFilter(tagObj){
    const {messages, filteredMessages, isFiltered} = this.state
    const newFiltered = messages.filter(tweet => {
      return (tweet.tweet.indexOf("#"+tagObj.value) !== -1)
    })
    console.log(newFiltered)

    let filtered
    if(isFiltered){
       filtered = filteredMessages.concat(newFiltered)
    }else{
      filtered = newFiltered
    }
    this.setState({filteredMessages:filtered, isFiltered: true})
  }

  clearFilter(){
    const {messages} = this.state
    this.setState({filteredMessages: messages, isFiltered: false})
  }
}

function mapStateToProps ({kafkaMessage}) {
  return { kafkaMessage }
}

function mapDispatchToProps (dispatch) {
  return bindActionCreators({socketKafkaMessage}, dispatch)
}
export default connect(mapStateToProps, mapDispatchToProps)(App)
