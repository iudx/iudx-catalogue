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
        // console.log(cls, match)
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

function plotGeoJSONs(geoJSONObject, _id){
    // console.log("plotting "+ geoJSONObject, _id, _id["id"])
    L.geoJSON(geoJSONObject, {
        pointToLayer: function (feature, latlng) {
                // return L.marker(latlng, {icon: myIcon});
                var _marker = L.marker(latlng);
                _marker.itemUUID = _id;
                _marker.on('click', markerOnClick);
                return _marker;
        },
        // filter: soffParkingFilter,
        // onEachFeature: onEachFeature
    }).addTo(markersLayer);
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

function activate_point_mode(_markerdetails) {
    $("#batch").hide();
    $("#resource_item_details").html(_markerdetails)
    $("#point").show();
}


function markerOnClick(e) {
    // var attributes = e.layer.properties;
    activate_point_mode(e.target.itemUUID);  
    sidebar.show();
    // alert(e.target.itemUUID);
    // console.log(attributes);
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
