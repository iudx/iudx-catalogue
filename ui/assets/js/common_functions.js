/*****Global variables declaration *****/
// var _provider_data;
// var _tags_data;
// var _resourceServerGroup_data;
// var _geoJSONObject;
// var _resourceId_data ;
var tags_set=[];


// Spinner by https://tobiasahlin.com/spinkit/
function get_spinner_html(){
    return `
    <div class="spinner">
      <div class="rect1"></div>
      <div class="rect2"></div>
      <div class="rect3"></div>
      <div class="rect4"></div>
      <div class="rect5"></div>
    </div>
    `
}

function get_api_encoded_attr_(_attr){
    return _attr;
}

function get_api_encoded_attribute_names(__tags, __rsg, __pvdr){
    var str = []
    if(__tags.length != 0){
        str.push(get_api_encoded_attr_("tags"))
    } if(__rsg.length != 0){
        str.push(get_api_encoded_attr_("resourceServerGroup"))
    } if(__pvdr.length != 0){
        str.push(get_api_encoded_attr_("provider"))
    }
    console.log(str.join(","))
    return str.join(",")
    
}

function get_api_encoded_attribute_values(__tags, __rsg, __pvdr){
    var str = []
    if(__tags.length != 0){
        str.push(get_api_encoded_attr_(__tags.join(",")))
    } if(__rsg.length != 0){
        str.push(get_api_encoded_attr_(__rsg.join(",")))
    } if(__pvdr.length != 0){
        str.push(get_api_encoded_attr_(__pvdr.join(",")))
    }
    console.log(str.join(","))
    return str.join(",")
}

function __get_latest_data(url) {
  return new Promise((resolve, reject) => {
    $.ajax({
      url: encodeURI(url),
      type: 'GET',
      success: function(data) {
        resolve(data)
      },
      error: function(error) {
        reject(error)
      },
      timeout: 30000 // sets timeout to 30 seconds
    })
  })
}

function _alertify(header_msg, body_msg){
    alertify.alert(body_msg);
    $(".ajs-header").html(header_msg);
}

function display_latest_data(e, ele) {
    e.preventDefault();   // use this to NOT go to href site
    _alertify("Getting Data...", get_spinner_html())
    __get_latest_data($(ele).attr("href"))
      .then(data => {
        _alertify("Success!!!", '<pre id="custom_alertbox">'+jsonPrettyHighlightToId(JSON.parse(data))+'</pre>')
      })
      .catch(error => {
        _alertify("Error!!!",'<pre id="custom_alertbox">: ' + error["statusText"]+ '</pre>');
      })
}

function get_filtered_url(__filter_url){
    if(__filter_url == `attribute-name=((""))&attribute-value=((""))`){
        return ""
    }else{
        return "&" + __filter_url
    }
}

function toast_alert(__msg, __msg_type){
    $.toast({
        text: __msg,
        position: 'mid-center',
        hideAfter: 1800,
        loader: false,  // Whether to show loader or not. True by default
        loaderBg: '#1abc9c',
        bgColor: '#1abc9c',
        showHideTransition: 'fade', // fade, slide or plain
        allowToastClose: false, // Boolean value true or false
        icon: __msg_type // Type of toast icon  
    })
}

function reset_filter(__input_name){
    $.each($(`input[name='`+__input_name+`']:checked`), function(){            
        $(this).removeAttr("checked");
    });
    var category = "";
    if (__input_name == "taglists"){
        category = "Tag"
    }else if (__input_name == "resource_server_group"){
        category = "Resource Server Group"
    }else if(__input_name == "provider"){
        category = "Provider"
    }

    toast_alert(category + ' filter has been cleared', 'success')
}
 function showDetails(){
     console.log("print this...")
     $('#_batch').hide();
     $('#point').show();
 }

function get_selected_values_framed_url(){
    var value = getSelectedValuesCheckbox();
    var tags = value.tags;
    var rsg = value.rsg;
    var provider = value.provider;
    console.log(tags, rsg , provider)

    var __filter_url = ""

    if(tags.length == 0 && rsg.length == 0 && provider.length == 0){
    __filter_url=`attribute-name=((""))&attribute-value=((""))`
    }else{
    // console.log("else...")
    var _attr_names = get_api_encoded_attribute_names(tags, rsg, provider) 
    // console.log(_attr_names)
    var _attr_values = get_api_encoded_attribute_values(tags, rsg, provider)
    // console.log(_attr_values)
    __filter_url=`attribute-name=((`+ _attr_names +`))&attribute-value=((`+ _attr_values +`))`+get_geo_shape_url(geo_shape)
    }
    return __filter_url;
}

function get_geo_shape_url(__geo_shape){
    var _url=""
    if(__geo_shape != null){
        if(__geo_shape['type'] == 'circle'){
            _url= "&lat=" + __geo_shape.value.center_point["lat"] + "&lon=" + __geo_shape.value.center_point["lng"] + "&radius=" + __geo_shape.value.radius
        }else if(__geo_shape['type'] == 'marker'){
            _url= "&lat=" + __geo_shape.value.center_point["lat"] + "&lon=" + __geo_shape.value.center_point["lng"]
        }else if(__geo_shape['type'] == 'polygon'){
            _url= "&geometry=polygon(("+ __geo_shape.value.points + ","+__geo_shape.value.points[0]+"))&relation=within"
        }else if(__geo_shape['type'] == 'rectangle'){
            _url = "&bbox=" + __geo_shape.value.bbox_points + "&relation=within"
        }else if(__geo_shape['type'] == 'polyline'){
            _url = "&bbox=" + __geo_shape.value.bbox_points + "&relation=within"
        }
    }

    return _url;
}

function _get_latest_data(_resource_id, _token){
    console.log(_token)
    $.ajax({
      url: "https://pune.iudx.org.in/api/1.0.0/resource/search/safetypin/18.56581555/73.77567708/10",
      type: 'get',
      headers: {"token": _token},
      success: function (data) {
        // alert("Success! \n"+data)
        // display_json_response_in_modal(data)
        _alertify("Success!!!", '<pre id="custom_alertbox">'+jsonPrettyHighlightToId(JSON.parse(data))+'</pre>');
      },
      error: _alertify("Error!!!", '<pre id="custom_alertbox">: Please try some time later. Server is facing some problems at this moment.</pre>')
    })
}

function _get_security_based_latest_data_link(_resource_id, _resourceServerGroup, _rid, token){
    if(_resource_id=="rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/safetipin/safetipin/safetyIndex"){
        return `<button class="btn btn-secondary" onclick="_get_latest_data('`+_resource_id+`','`+token+`')">Get Full Latest Data</button>`
    }else{
        return `<a href="`+ get_latest_data_url(_resource_id,_resourceServerGroup,_rid) +`" class="data-modal"  onclick="display_latest_data(event, this)">Get Latest Data</a>`
    }
}

function request_access_token(resource_id, resourceServerGroup, rid) {
    console.log(resource_id)
    $.ajax({
      url: "https://auth.iudx.org.in/auth/v1/token",
      type: 'post',
      dataType: 'json',
      contentType: 'application/json',
      data: JSON.stringify({"resource-id": resource_id}),
      success: function (data) {
        // console.log(data.token)
        
        // $('#token_section_'+resource_id_to_html_id(resource_id)).html($('#token_section_'+resource_id_to_html_id(resource_id)).html());
        $('#token_section_'+resource_id_to_html_id(resource_id)).html(
                                                                        `<b>Token</b>: <span id="token_value_`+resource_id_to_html_id(resource_id)+`">` + data.token + `</span>`
                                                                        + `&nbsp;&nbsp;&nbsp;<button class="btn copy-btn" onclick="copyToClipboard('`+resource_id_to_html_id(resource_id)+`')"> Copy Token <img class="secure_icon svg-white" src="../assets/img/icons/copy_white.svg"></button> <br> `
                                                                        + _get_security_based_latest_data_link(resource_id,resourceServerGroup, rid, data.token))
        
        _alertify("Success!!!", "Token received.<br>You are now authenticated to access the non-public data.")
        // _alertify("Success!!!", "Token received: " + data.token)
        $('#token_section_'+resource_id_to_html_id(resource_id)).toggle();
            
      },
      error: function (jqXHR, exception) {
        _alertify("Error", "Unauthorized access! Please get a token.")
      }
    });
}

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
// function geoquery_circle(_lat,_lng, _radius) {
//       return new Promise(function(resolve, reject) {
//         // resourceClass =  $.unique(data.map(function (d) {
//         //         return d.accessInformation[0].accessVariables.resourceClass
//         //     }));
//          // markersLayer.clearLayers();
         
// 		      $.get("/search/catalogue/attribute?bounding-type=circle&lat="+ _lat +"&long="+ _lng +"&radius="+ _radius, function(data) {

//             data=JSON.parse(data)
//             for (var i = data.length - 1; i >= 0; i--) {
//                 if(data[i].hasOwnProperty('geoJsonLocation')){
//                     // myLayer.addData(data[i]['geoJsonLocation']);
//                     plotGeoJSONs(data[i]["geoJsonLocation"])
//                 }
//             }
//             // DATA=data
//             resolve(data);
//         });
//       });
// }

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

function plotGeoJSONs(geoJSONObject, _id, plot_id,_resourceServerGroup,_resourceId,_tags,_provider){
    //console.log(_resourceServerGroup)
    // //console.log("plotting "+ geoJSONObject, _id, _id["id"])
    // // console.log(geoJSONObject, color_count)
    // _provider_data = _provider;
    // _tags_data = _tags;
    // _resourceServerGroup_data =_resourceServerGroup;
    // _geoJSONObject = geoJSONObject;
    // _resourceId_data = _resourceId;

    //console.log(geoJSONObject)
    
    if(geoJSONObject["type"]=="Polygon"){
        
        console.log("Printing Polygon....")
        color_count=color_count+1
        var _color=getRandomColor()
        
        var div = $('div.info.legend');

        //console.log(_resourceServerGroup, div)
        if(_resourceServerGroup=="crowd-sourced-changebhai"){
        // loop through our density intervals and generate a label with a colored square for each interval
            console.log("changeBhai")
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
                        console.log(feature.properties);
                        // return L.marker(latlng, {icon: getOfficeIcon()});
                       
                        // <a href='/catalogue/v1/items/"+plot_id+"'>Get Catalogue-item-details</a><br/>
                        var customPopup = "<a href='https://pune.iudx.org.in/api/1.0.0/resource/latest/"+_resourceServerGroup+"/"+_resourceId + "' class='data-modal'  onclick='display_latest_data(event, this)'>Get latest-data</a>";
                        if(_resourceServerGroup==='streetlight-feeder-sree'){
                            //console.log("street")
                            var _marker = L.marker(latlng,{icon: getStreetlightIcon()}).addTo(map);
                            _marker.itemUUID = _id;
                        ////console.log(_marker.itemUUID);
                        _marker.on('click', markerOnClick);
                        _marker.bindPopup(customPopup)
                        console.log(_marker)
                        return _marker;
                        }
                        if(_resourceServerGroup==='aqm-bosch-climo'){
                            //console.log("aqm")
                            var _marker = L.marker(latlng,{icon: getAirQualityIcon()}).addTo(map);
                            _marker.itemUUID = _id;
                        ////console.log(_marker.itemUUID);
                        _marker.on('click', markerOnClick);
                        _marker.bindPopup(customPopup)
                        console.log(_marker)
                        return _marker;
                        }
                        if(_resourceServerGroup==='flood-sensor'){
                            //console.log("flood")
                            var _marker = L.marker(latlng,{icon: getFloodSensorIcon()}).addTo(map);
                            _marker.itemUUID = _id;
                        ////console.log(_marker.itemUUID);
                        _marker.on('click', markerOnClick);
                        _marker.bindPopup(customPopup)
                        console.log(_marker)
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
                //filter: filter_byTags,
                // onEachFeature: onEachFeature
            }).addTo(markersLayer);
            console.log("1111111111111111");
    
    }
    else if(geoJSONObject["type"]=="Point"){
           // console.log("Printing Point....")
            L.geoJSON(geoJSONObject, {
                pointToLayer: function (feature, latlng) {
                    
                        // return L.marker(latlng, {icon: getOfficeIcon()});
                       
                        // <a href='/catalogue/v1/items/"+plot_id+"'>Get Catalogue-item-details</a><br/>
                        var customPopup = "<a href='https://pune.iudx.org.in/api/1.0.0/resource/latest/"+_resourceServerGroup+"/"+_resourceId+"' class='data-modal'  onclick='display_latest_data(event, this)'>Get latest-data</a>";
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
               // filter: filter_byTags,
                // onEachFeature: onEachFeature
            }).addTo(markersLayer);
   // console.log("22222222222")
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
    $("#_batch").show();
    //console.log("batch mode")
    hide_menu_icon();
}

function activate_point_mode(_id) {
    // console.log(1,_id)
    $("#_batch").hide();
    // console.log(2,_id)
   // console.log("called")
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


function getSelectedValuesCheckbox(){
    var _tags = [];
    var _rsg = [];
    var _pr = [];
    
    $.each($("input[name='taglists']:checked"), function(){            
        _tags.push($(this).val());
    });

    $.each($("input[name='resource_server_group']:checked"), function(){            
        _rsg.push($(this).val());
    });

    $.each($("input[name='provider']:checked"), function(){            
        _pr.push($(this).val());
    });
    //alert("My taglists are: " + _tags.join(", ")+"and My resource group are:" +_rsg.join(", "));
  
      return values =
    {
        "tags" : _tags,
        "rsg"  : _rsg,
        "provider"   : _pr
    }

}
