import React, { Component } from 'react';
import classnames from 'classnames';
import { TagCloud } from "react-tagcloud";

const colors = ["green", "#a53545", "#089ba3"]

class TwitterTagCloud extends Component {
  constructor(props){
    super(props)
    this.getFormattedData = this.getFormattedData.bind(this)
  }

  customRenderer(tag, size, color) {
    return(
      <span key={tag.value}
            className="tagStyle"
            style={{
              fontSize: `${size}em`,
              border: `4px solid ${color}`
            }}>{tag.value}</span>
    )
  }

  render() {
    const formattedData = this.getFormattedData()
    return (
      <TagCloud tags={formattedData}
            minSize={1}
            maxSize={2}
            renderer={this.customRenderer}
            onClick={this.props.onTagFilter}/>
    )
  }

  getFormattedData(){
    const formattedData = this.props.data.map((message, index) => {
      const color = this.getColorIndex(message.positiveSentimentCount, message.negativeSentimentCount, message.neutralSentimentCount)
      return {
        value: message.tag,
        count: message.count,
        key: message.tag+index,
        color: colors[color]
      }
    })
    return formattedData
  }

  getColorIndex(pCount, nCount, neCount){
    if(pCount > nCount && pCount > neCount){
      return 0
    }else if(nCount > pCount && nCount > neCount){
      return 1
    }else{
      return 2
    }
  }
}

export default TwitterTagCloud
