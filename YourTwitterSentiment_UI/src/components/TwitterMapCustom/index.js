import React, { Component } from 'react';
import classnames from 'classnames';
import GoogleMap from 'google-map-react';
import MapMarker from '../MapMarker'

const API_KEY= 'AIzaSyAfKl5fGfL_Iu8CocKc-VHDH_FDk0pQCVQ'
const style = { width: '95%', height: '70%' }

class TwitterMapCustom extends Component {
  constructor(props){
    super(props)
    this.state = {
        center: [0,20],
        zoom: 1,
        options:{
          minZoom: 1,
          maxZoom: 2
        },
        greatPlaces: [
          {id: 'A', lat: 10.955413, lng: 40.337844},
          {id: 'B', lat: 40.724, lng: 80.080}
        ],
        hoverKey: null,
        clickKey: ''
    }
    this.onChange = this.onChange.bind(this)
    this.onBoundsChange = this.onBoundsChange.bind(this)
    this.onChildClick = this.onChildClick.bind(this)
    this.onChildMouseEnter = this.onChildMouseEnter.bind(this)
    this.onChildMouseLeave = this.onChildMouseLeave.bind(this)
    this.onCenterChange = this.onCenterChange.bind(this)
    this.onZoomChange = this.onZoomChange.bind(this)
    this.onHoverKeyChange = this.onHoverKeyChange.bind(this)
  }

  onChange = ({center, zoom}) => {
    this.setState({
      center: center,
      zoom: zoom
    });
  }

  render() {
    const places = this.state.greatPlaces
          .map(place => {
            const {id, ...coords} = place;

            return (
              <MapMarker
                key={id}
                {...coords}
                text={id}
                hover={this.props.hoverKey === id} />
            );
          });
    return (
      <div style={{width: '100%', height: '95%'}}>
        <GoogleMap
          center={this.state.center}
          zoom={this.state.zoom}
          options={this.state.options}
          hoverDistance={10}
          onChange={this.onChange}
          onBoundsChange={this.onBoundsChange}
          onChildClick={this.onChildClick}
          onChildMouseEnter={this.onChildMouseEnter}
          onChildMouseLeave={this.onChildMouseLeave}>
         {places}
       </GoogleMap>
      </div>
    )
  }

  onCenterChange(center){
    this.setState({center})
  }

  onZoomChange(zoom){
    this.setState({zoom})
  }

  onHoverKeyChange(hoverKey){
    this.setState({hoverKey})
  }

 onBoundsChange = (center, zoom /* , bounds, marginBounds */) => {
   this.onCenterChange(center);
   this.onZoomChange(zoom);
 }

 onChildClick = (key, childProps) => {
   console.log(key+" clicked")
   this.onCenterChange([childProps.lat, childProps.lng]);
 }

 onChildMouseEnter = (key /*, childProps */) => {
   console.log("mouse enter: "+key)
   this.onHoverKeyChange(key);
 }

 onChildMouseLeave = (/* key, childProps */) => {
   this.onHoverKeyChange(null);
 }
}

export default TwitterMapCustom
