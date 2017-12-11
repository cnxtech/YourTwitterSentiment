import React, {Component} from 'react';

import {markerStyle, markerStyleHover} from './markerStyles.js';

export default class MapMarker extends Component {

  constructor(props) {
    super(props);
  }

  render() {
    const style = this.props.$hover ? markerStyleHover : markerStyle;

    return (
       <div className="hint hint--html hint--info hint--top" style={style}>
          <div>{this.props.text}</div>
          <div style={{width: 80}} className="hint__content">
            Сlick me
          </div>
       </div>
    );
  }
}
