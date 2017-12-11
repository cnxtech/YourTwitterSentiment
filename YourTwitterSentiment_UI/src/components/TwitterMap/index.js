import React, { Component } from 'react';
import classnames from 'classnames';
import {Map, InfoWindow, Marker, GoogleApiWrapper, withGoogleMap} from 'google-maps-react';

const API_KEY= 'AIzaSyAfKl5fGfL_Iu8CocKc-VHDH_FDk0pQCVQ'

const style = { width: '100%', height: '75%' }

const windowStyle = { width: '20px', height: '10px' }

const markerImageUrls = ["../../../static/images/positivemark.png",
                         "../../../static/images/negativemark.png",
                         "../../../static/images/neutralmark.png"]

class TwitterMap extends Component {
  constructor(props){
    super(props)
    this.state = {
        showingInfoWindow: false,
        activeMarker: {},
        selectedPlace: {},
        hoveredPlace: {},
        hoveredMarker: {},
        showHoverInfoWindow: false
   }
    this.onMarkerClick = this.onMarkerClick.bind(this)
    this.onMapClick = this.onMapClick.bind(this)
    this.infoWindowHasClosed = this.infoWindowHasClosed.bind(this)
    this.getMarkers = this.getMarkers.bind(this)
    this.onMouseoverMarker = this.onMouseoverMarker.bind(this)
  }

  render() {
    const {google, maps} = this.props
    const markers = this.getMarkers()

    return (
      <Map
        google={this.props.google}
        zoom={2}
        style={style}
        initialCenter={{lat: 19, lng: 20}}
        onClick={this.onMapClick}>
        {markers}
        <InfoWindow
          style ={windowStyle}
          marker={this.state.activeMarker}
          visible={this.state.showingInfoWindow}
          onClose={this.onInfoWindowClose}>
            {this.renderInfo(this.state.selectedPlace)}
        </InfoWindow>
      </Map>
    )
  }

  getMarkers(){
    const tweetSentiments = this.props.data
    const markers = tweetSentiments.map((tweet, index) => {
      const title = tweet.userName + " tweeted: "+ tweet.tweet
      const markerUrl = tweet.sentiment == 'POSITIVE' ? markerImageUrls[0] :
                        (tweet.sentiment == 'NEGATIVE' ? markerImageUrls[1] : markerImageUrls[2])
      return (
        <Marker
          key = {index}
          title={title}
          name={tweet.sentiment}
          position={{lat: tweet.latitude, lng: tweet.longitude}}
          onClick={this.onMarkerClick}
          onMouseover={this.onMouseoverMarker}
          icon={{
            url: markerUrl
          }}/>
      )
    })
    return markers
  }

  renderInfo(selectedPlace){
    const sentiment = selectedPlace.name
    const style = sentiment == 'POSITIVE' ? 'alert alert-success' : (sentiment == 'NEGATIVE' ? 'alert alert-danger' : 'alert alert-info')
    return(
      <div className={style}>
        {selectedPlace.title}
      </div>
    )
  }

  onMouseoverMarker(props, marker, e) {
    const {showHoverInfoWindow} = this.state
    // if(showHoverInfoWindow){
    //   this.setState({
    //     hoveredPlace: null,
    //     hoveredMarker: null,
    //     showHoverInfoWindow: false
    //   })
    // }else{
    //   this.setState({
    //     hoveredPlace: props,
    //     hoveredMarker: marker,
    //     showHoverInfoWindow: true
    //   })
    // }
  }

  onMarkerClick(props, marker, e) {
    this.setState({
      selectedPlace: props,
      activeMarker: marker,
      showingInfoWindow: true
    });
  }

  onMapClick(props) {
    if (this.state.showingInfoWindow) {
      this.setState({
        showingInfoWindow: false,
        activeMarker: null
      })
    }
  }

  infoWindowHasClosed(){
    this.setState({
      showingInfoWindow: false,
      activeMarker: null
    })
  }

}

export default GoogleApiWrapper({
  apiKey: API_KEY
})(TwitterMap)
