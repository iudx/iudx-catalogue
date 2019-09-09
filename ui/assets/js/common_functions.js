function urlify(text) {
  var urlRegex = /(https?:\/\/[^"]+)/g;
  return text.replace(urlRegex, '<a href="$1" target="_blank" style="text-decoration:underline">$1</a>')
}

function jsonPrettyHighlightToId(jsonobj) {

    var json = JSON.stringify(jsonobj, undefined, 2);

    json = json.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    json = json.replace(/("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?)/g, function(match) {
        var cls = 'color: darkorange;';
        if (/^"/.test(match)) {
            if (/:$/.test(match)) {
                cls = 'color: red;';
            } else {
                cls = 'color: green;';
            }
        } else if (/true|false/.test(match)) {
            cls = 'color: blue;';
        } else if (/null/.test(match)) {
            cls = 'color: magenta;';
        }
        // //console.log(cls, match)
        return '<span style="' + cls + '">' + urlify(match) + '</span>';
    });
    // return urlify(json);
    return json;
}

function jsonPrettyHighlightToIdwithBR(jsonobj) {

    var json = JSON.stringify(jsonobj, undefined, 2);

    json = json.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    // console.log(json.replace(/\n/g, "<br />"))
    json=json.replace(/\n/g, "<br />")
    json = json.replace(/("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?)/g, function(match) {
        var cls = 'color: darkorange;';
        if (/^"/.test(match)) {
            if (/:$/.test(match)) {
                cls = 'color: red;';
            } else {
                cls = 'color: green;';
            }
        } else if (/true|false/.test(match)) {
            cls = 'color: blue;';
        } else if (/null/.test(match)) {
            cls = 'color: magenta;';
        }
        // //console.log(cls, match)
        return '<span style="' + cls + '">' + urlify(match) + '</span>';
    });
    // return urlify(json);
    return json;
}


//ajax call to get resource-items using /search/catalogue/attribute for geoquery type=circle
// e.g. https://localhost:8443/search/catalogue/attribute?location={bounding-type=circle&lat=79.01234&long=78.33579&radius=1}
function geoquery_circle(_lat,_lng, _radius) {
      return new Promise(function(resolve, reject) {
        // resourceClass =  $.unique(data.map(function (d) {
        //         return d.accessInformation[0].accessVariables.resourceClass
        //     }));
         // markersLayer.clearLayers();
         
		      $.get("/search/catalogue/attribute?bounding-type=circle&lat="+ _lat +"&long="+ _lng +"&radius="+ _radius, function(data) {

            data=JSON.parse(data)
            for (var i = data.length - 1; i >= 0; i--) {
                if(data[i].hasOwnProperty('geoJsonLocation')){
                    // myLayer.addData(data[i]['geoJsonLocation']);
                    plotGeoJSONs(data[i]["geoJsonLocation"])
                }
            }
            // DATA=data
            resolve(data);
        });
      });
}

function settimeout(time_milli_seconds) {
      return new Promise(function(resolve, reject) {
        setTimeout(function () {
                sidebar.show();
            }, time_milli_seconds);
      });
}

function onEachFeature(feature, layer) {
    layer.bindPopup(feature.properties.name);
}

var colors=["#1abc9c", '#f1c40f']//, '#9b59b6']//, '#e67e22', '#f39c12']

var color_count=-1;

function getRandomColor(){
 var color =  "#" + (Math.random() * 0xFFFFFF << 0).toString(16);
 return color;
}

function plotGeoJSONs(geoJSONObject, _id, plot_id,_resourceServerGroup,_resourceId){
    //console.log(_resourceServerGroup)
    // //console.log("plotting "+ geoJSONObject, _id, _id["id"])
    // console.log(geoJSONObject, color_count)
    
    if(geoJSONObject["type"]=="Polygon"){
        color_count=color_count+1
        var _color=getRandomColor()
        
        var div = $('div.info.legend');

        console.log(_resourceServerGroup, div)
        if(_resourceServerGroup=="crowd-sourced-changebhai"){
        // loop through our density intervals and generate a label with a colored square for each interval

            div.innerHTML +=  
            '<span style="background:' + _color + '"></span> ' +
              'ChangeBhai' + '<br>';

        }else if(_resourceServerGroup=="crowd-sourced-safetipin"){
            div.innerHTML +=  
            '<span style="background:' + _color + '"></span> ' +
              'safetiPin' + '<br>';

        }else if(_resourceServerGroup=="traffic-incidents"){
            div.innerHTML +=  
            '<span style="background:' + _color + '"></span> ' +
              'TomTom' + '<br>';
        }


        L.geoJSON(geoJSONObject, {
                style: {
                        // fillColor: colors[color_count],
                        // fillColor: _color,
                        fillColor: colors[color_count%3],
                        weight: 2,
                        opacity: 1,
                        // color: 'white',
                        // dashArray: '3',
                        fillOpacity: 0.5
                      },
                pointToLayer: function (feature, latlng) {
                        // return L.marker(latlng, {icon: getOfficeIcon()});
                        
                        // <a href='/catalogue/v1/items/"+plot_id+"'>Get Catalogue-item-details</a><br/>
                        var customPopup = "<a href='https://pune.iudx.org.in/api/1.0.0/resource/latest/"+_resourceServerGroup+"/"+_resourceId+"' target='_blank'>Get latest-data</a>";
                        if(_resourceServerGroup==='streetlight-feeder-sree'){
                            //console.log("street")
                            var _marker = L.marker(latlng,{icon: getStreetlightIcon()}).addTo(map);
                            _marker.itemUUID = _id;
                        ////console.log(_marker.itemUUID);
                        _marker.on('click', markerOnClick);
                        _marker.bindPopup(customPopup)
                        return _marker;
                        }
                        if(_resourceServerGroup==='aqm-bosch-climo'){
                            //console.log("aqm")
                            var _marker = L.marker(latlng,{icon: getAirQualityIcon()}).addTo(map);
                            _marker.itemUUID = _id;
                        ////console.log(_marker.itemUUID);
                        _marker.on('click', markerOnClick);
                        _marker.bindPopup(customPopup)
                        return _marker;
                        }
                        if(_resourceServerGroup==='flood-sensor'){
                            //console.log("flood")
                            var _marker = L.marker(latlng,{icon: getFloodSensorIcon()}).addTo(map);
                            _marker.itemUUID = _id;
                        ////console.log(_marker.itemUUID);
                        _marker.on('click', markerOnClick);
                        _marker.bindPopup(customPopup)
                        return _marker;
                        }
                        if(_resourceServerGroup==='wifi-hotspot'){
                            //console.log("wifi")
                            var _marker = L.marker(latlng,{icon: getWifiHotspotIcon()}).addTo(map);
                            _marker.itemUUID = _id;
                        ////console.log(_marker.itemUUID);
                        _marker.on('click', markerOnClick);
                        _marker.bindPopup(customPopup)
                        return _marker;
                        }
                        if(_resourceServerGroup==='itms'){
                            //console.log("itms")
                            var _marker = L.marker(latlng,{icon: getITMSIcon()}).addTo(map);
                            _marker.itemUUID = _id;
                        ////console.log(_marker.itemUUID);
                        _marker.on('click', markerOnClick);
                        _marker.bindPopup(customPopup)
                        return _marker;
                        }
                        if(_resourceServerGroup==='changebhai'){
                            //console.log("change")
                            var _marker = L.marker(latlng,{icon: getChangebhaiIcon()}).addTo(map);
                            _marker.itemUUID = _id;
                        ////console.log(_marker.itemUUID);
                        _marker.on('click', markerOnClick);
                        _marker.bindPopup(customPopup)
                        return _marker;
                        }
                        if(_resourceServerGroup==='safetypin'){
                            //console.log("safety")
                            var _marker = L.marker(latlng,{icon: getSafetypinIcon()}).addTo(map);
                            _marker.itemUUID = _id;
                        ////console.log(_marker.itemUUID);
                        _marker.on('click', markerOnClick);
                        _marker.bindPopup(customPopup)
                        return _marker;
                        }
                        if(_resourceServerGroup==='traffic-incidents'){
                            //console.log("aqm")
                            var _marker = L.marker(latlng,{icon: getAirQualityIcon()}).addTo(map);
                            _marker.itemUUID = _id;
                        ////console.log(_marker.itemUUID);
                        _marker.on('click', markerOnClick);
                        _marker.bindPopup(customPopup)
                        return _marker;
                        }
                        
                        // //console.log(_marker)
                        // ////console.log(_id);
                        // ////console.log(geoJSONObject);
                        // _marker.itemUUID = _id;
                        // ////console.log(_marker.itemUUID);
                        // _marker.on('click', markerOnClick);
                        // _marker.bindPopup(customPopup)
                        // return _marker;
                },
                // filter: soffParkingFilter,
                // onEachFeature: onEachFeature
            }).addTo(markersLayer);
    
    }else if(geoJSONObject["type"]=="Point"){

            L.geoJSON(geoJSONObject, {
                pointToLayer: function (feature, latlng) {
                        // return L.marker(latlng, {icon: getOfficeIcon()});
                        
                        // <a href='/catalogue/v1/items/"+plot_id+"'>Get Catalogue-item-details</a><br/>
                        var customPopup = "<a href='https://pune.iudx.org.in/api/1.0.0/resource/latest/"+_resourceServerGroup+"/"+_resourceId+"' target='_blank'>Get latest-data</a>";
                        if(_resourceServerGroup==='streetlight-feeder-sree'){
                            //console.log("street")
                            var _marker = L.marker(latlng,{icon: getStreetlightIcon()}).addTo(map);
                            _marker.itemUUID = _id;
                        ////console.log(_marker.itemUUID);
                        _marker.on('click', markerOnClick);
                        _marker.bindPopup(customPopup)
                        return _marker;
                        }
                        if(_resourceServerGroup==='aqm-bosch-climo'){
                            //console.log("aqm")
                            var _marker = L.marker(latlng,{icon: getAirQualityIcon()}).addTo(map);
                            _marker.itemUUID = _id;
                        ////console.log(_marker.itemUUID);
                        _marker.on('click', markerOnClick);
                        _marker.bindPopup(customPopup)
                        return _marker;
                        }
                        if(_resourceServerGroup==='flood-sensor'){
                            //console.log("flood")
                            var _marker = L.marker(latlng,{icon: getFloodSensorIcon()}).addTo(map);
                            _marker.itemUUID = _id;
                        ////console.log(_marker.itemUUID);
                        _marker.on('click', markerOnClick);
                        _marker.bindPopup(customPopup)
                        return _marker;
                        }
                        if(_resourceServerGroup==='wifi-hotspot'){
                            //console.log("wifi")
                            var _marker = L.marker(latlng,{icon: getWifiHotspotIcon()}).addTo(map);
                            _marker.itemUUID = _id;
                        ////console.log(_marker.itemUUID);
                        _marker.on('click', markerOnClick);
                        _marker.bindPopup(customPopup)
                        return _marker;
                        }
                        if(_resourceServerGroup==='itms'){
                            //console.log("itms")
                            var _marker = L.marker(latlng,{icon: getITMSIcon()}).addTo(map);
                            _marker.itemUUID = _id;
                        ////console.log(_marker.itemUUID);
                        _marker.on('click', markerOnClick);
                        _marker.bindPopup(customPopup)
                        return _marker;
                        }
                        if(_resourceServerGroup==='changebhai'){
                            //console.log("change")
                            var _marker = L.marker(latlng,{icon: getChangebhaiIcon()}).addTo(map);
                            _marker.itemUUID = _id;
                        ////console.log(_marker.itemUUID);
                        _marker.on('click', markerOnClick);
                        _marker.bindPopup(customPopup)
                        return _marker;
                        }
                        if(_resourceServerGroup==='safetypin'){
                            //console.log("safety")
                            var _marker = L.marker(latlng,{icon: getSafetypinIcon()}).addTo(map);
                            _marker.itemUUID = _id;
                        ////console.log(_marker.itemUUID);
                        _marker.on('click', markerOnClick);
                        _marker.bindPopup(customPopup)
                        return _marker;
                        }
                        
                        // //console.log(_marker)
                        // ////console.log(_id);
                        // ////console.log(geoJSONObject);
                        // _marker.itemUUID = _id;
                        // ////console.log(_marker.itemUUID);
                        // _marker.on('click', markerOnClick);
                        // _marker.bindPopup(customPopup)
                        // return _marker;
                },
                // filter: soffParkingFilter,
                // onEachFeature: onEachFeature
            }).addTo(markersLayer);
    
    }
}


function get_latest_data_url(id, rsg, rid){
    if(id=="rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/safetipin/safetipin/safetyIndex"){
        return 'https://pune.iudx.org.in/api/1.0.0/resource/search/safetypin/18.56581555/73.77567708/10'
    }else{
        return `https://pune.iudx.org.in/api/1.0.0/resource/latest/`+rsg+`/`+rid
    }
}

function show_menu_icon() {
    $("#menu-bar-icon").show(500);
}

function hide_menu_icon() {
    $("#menu-bar-icon").hide(500);
}

function activate_batch_mode() {
    $("#point").hide();
    $("#batch").show();
    hide_menu_icon();
}

function activate_point_mode(_id) {
    // console.log(1,_id)
    $("#batch").hide();
    // console.log(2,_id)
    console.log("called")
    show_details(_id)
    $("#point").show();
}

function resource_id_to_html_id(resource_id){
    var replace_with = "_";
    return resource_id.replace(/\/|\.|\s|\(|\)|\<|\>|\{|\}|\,|\"|\'|\`|\*|\;|\+|\!|\#|\%|\^|\&|\=|\₹|\$|\@/g,replace_with)
}

function markerOnClick(e) {
    // var attributes = e.layer.properties;
    ////console.log(e.target.itemUUID)
    activate_point_mode(e.target.itemUUID);  
    sidebar.show();
    // alert(e.target.itemUUID);
    // //console.log(attributes);
    // do some stuff…
}


function getPuneLatLng(){
    return [18.5204, 73.8567]
}

function getOfficeIcon(){
    var officeIcon = L.icon({
        iconUrl: 'https://image.flaticon.com/icons/svg/167/167707.svg',
        // shadowUrl: 'leaf-shadow.png',

        iconSize:     [38, 95], // size of the icon
        shadowSize:   [50, 64], // size of the shadow
        iconAnchor:   [22, 94], // point of the icon which will correspond to marker's location
        shadowAnchor: [4, 62],  // the same for the shadow
        popupAnchor:  [-3, -76] // point from which the popup should open relative to the iconAnchor
    });
    return officeIcon;
}

function getStreetlightIcon(){
    var streetlightIcon = L.icon({
        iconUrl: 'https://img.icons8.com/color/48/000000/street-light.png',
        // shadowUrl: 'leaf-shadow.png',

        iconSize:      [25, 41], // size of the icon
        iconAnchor:    [12, 41], // point of the icon which will correspond to marker's location
        popupAnchor:   [1, -34], // point from which the popup should open relative to the iconAnchor
        shadowSize:    [41, 41]  // size of the shadow
    });
    return streetlightIcon;
}

function getAirQualityIcon(){
    var AirQualityIcon = L.icon({
        iconUrl: 'https://img.icons8.com/color/48/000000/air-quality.png',
        // shadowUrl: 'leaf-shadow.png',

        iconSize:      [25, 41], // size of the icon
        iconAnchor:    [12, 41], // point of the icon which will correspond to marker's location
        popupAnchor:   [1, -34], // point from which the popup should open relative to the iconAnchor
        shadowSize:    [41, 41]  // size of the shadow
    });
    return AirQualityIcon;
}

function getFloodSensorIcon(){
    var FloodSensorIcon = L.icon({
        iconUrl: 'https://img.icons8.com/office/16/000000/sensor.png',
        // shadowUrl: 'leaf-shadow.png',

        iconSize:      [25, 41], // size of the icon
        iconAnchor:    [12, 41], // point of the icon which will correspond to marker's location
        popupAnchor:   [1, -34], // point from which the popup should open relative to the iconAnchor
        shadowSize:    [41, 41]  // size of the shadow
    });
    return FloodSensorIcon;
}

function getWifiHotspotIcon(){
    var WifiHotspotIcon = L.icon({
        iconUrl: 'https://img.icons8.com/flat_round/64/000000/wi-fi-connected.png',
        // shadowUrl: 'leaf-shadow.png',

        iconSize:      [25, 41], // size of the icon
        iconAnchor:    [12, 41], // point of the icon which will correspond to marker's location
        popupAnchor:   [1, -34], // point from which the popup should open relative to the iconAnchor
        shadowSize:    [41, 41]  // size of the shadow
    });
    return WifiHotspotIcon;
}

function getITMSIcon(){
    var ITMSIcon = L.icon({
        iconUrl: 'https://img.icons8.com/ultraviolet/40/000000/marker.png',
        // shadowUrl: 'leaf-shadow.png',
        iconSize:      [25, 41], // size of the icon
        iconAnchor:    [12, 41], // point of the icon which will correspond to marker's location
        popupAnchor:   [1, -34], // point from which the popup should open relative to the iconAnchor
        shadowSize:    [41, 41]  // size of the shadow
    });
    return ITMSIcon;
}

function getChangebhaiIcon(){
    var changebhaiIcon = L.icon({
        iconUrl: 'https://img.icons8.com/color/48/000000/marker.png',
        // shadowUrl: 'leaf-shadow.png',

        iconSize:      [25, 41], // size of the icon
        iconAnchor:    [12, 41], // point of the icon which will correspond to marker's location
        popupAnchor:   [1, -34], // point from which the popup should open relative to the iconAnchor
        shadowSize:    [41, 41]  // size of the shadow
    });
    return changebhaiIcon;
}

function getSafetypinIcon(){
    var safetypinIcon = L.icon({
        iconUrl: 'https://img.icons8.com/flat_round/64/000000/safety-pin--v2.png',
        // shadowUrl: 'leaf-shadow.png',

        iconSize:      [25, 41], // size of the icon
        iconAnchor:    [12, 41], // point of the icon which will correspond to marker's location
        popupAnchor:   [1, -34], // point from which the popup should open relative to the iconAnchor
        shadowSize:    [41, 41]  // size of the shadow
    });
    return safetypinIcon;
}
