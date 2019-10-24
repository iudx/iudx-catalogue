/*****Global variables declaration *****/
// var _provider_data;
// var _tags_data;
// var _resourceServerGroup_data;
// var _geoJSONObject;
// var _resourceId_data ;

var tags_set = [];

function get_icon_credits() {

    var str = `Various icons used in this web app have been taken from <a href="` + icon_attribution['site_link'] + `" target="_blank">` + icon_attribution['site'] + `</a> and belong to the following authors.<br><ul>`

    for (var i = 0; i < icon_attribution['author'].length; i++) {
        for (var key in icon_attribution['author'][i]) {
            str += `<li><a href="` + icon_attribution['author'][i][key] + `" target="_blank">` + key + `</a></li>`;
        }
    }

    str += "</ul>"

    _alertify("Icon Credits", str);
}

function get_icon_attribution_html(map_icon_attr) {
    // return `<span class="` + map_icon_attr + `">Icons made by <a href="`+icon_attribution['author_link']+`" target="_blank">`+icon_attribution['author']+`</a> from <a href="`+icon_attribution['site_link']+`" target="_blank">`+icon_attribution['site']+`</a>.</span>`
    return `<span class ="` + map_icon_attr + `">Icons from <a href="` + icon_attribution['site_link'] + `" target="_blank">` + icon_attribution['site'] + `</a> | <a href="#" onclick="get_icon_credits()">Credits</a></span>`
}

function getImageRsg(_resourceServerGroup) {
    return legends[_resourceServerGroup]
}

// Spinner by https://tobiasahlin.com/spinkit/
function get_spinner_html() {
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

function get_api_encoded_attr_(_attr) {
    return _attr;
}

function get_api_encoded_attribute_names(__tags, __rsg, __pvdr) {
    var str = []
    if (__tags.length != 0) {
        str.push(get_api_encoded_attr_("tags"))
    } if (__rsg.length != 0) {
        str.push(get_api_encoded_attr_("resourceServerGroup"))
    } if (__pvdr.length != 0) {
        str.push(get_api_encoded_attr_("provider"))
    }
    //console.log(str.join(","))
    return "(" + str.join(",") + ")"

}

function get_api_encoded_attribute_values(__tags, __rsg, __pvdr) {
    var str = []
    if (__tags.length != 0) {
        str.push(get_api_encoded_attr_("(" + __tags.join(",") + ")"))
    } if (__rsg.length != 0) {
        str.push(get_api_encoded_attr_("(" + __rsg.join(",") + ")"))
    } if (__pvdr.length != 0) {
        str.push(get_api_encoded_attr_("(" + __pvdr.join(",") + ")"))
    }
    //console.log(str.join(","))
    return "(" + str.join(",") + ")"
}

function __get_latest_data(__url, __rid) {
    return new Promise((resolve, reject) => {
        $.ajax({
            "url": __url,
            "async": true,
            "crossDomain": true,
            "processData": false,
            "method": 'POST',
            "headers": { "Content-Type": "application/json" },
            "data": JSON.stringify({
                "id": __rid,
                "options": "latest"
            }),
            // dataType: 'json',
            success: function (data) {
                resolve(data)
            },
            error: function (error) {
                reject(error)
            },
            timeout: 30000 // sets timeout to 30 seconds
        })
    })
}

new Date('2015-03-04T00:00:00.000Z'); // Valid Date

function get_temporal_query_time_in_iso_8601_format(__days){
    return new Promise((resolve, reject) => {
        var today = new Date();
        var last = new Date(today.getTime() - (__days * 24 * 60 * 60 * 1000));
        resolve(last.toISOString()+"/"+today.toISOString())
    })
}

function __get_temporal_data(__url, __rid, __days) {
    var time;
    return new Promise((resolve, reject) => {
        var time;
        get_temporal_query_time_in_iso_8601_format(__days)
        .then(data => {
            time = data
            $.ajax({
                "url": __url,
                "async": true,
                "crossDomain": true,
                "processData": false,
                "method": 'POST',
                "headers": { "Content-Type": "application/json" },
                "data": JSON.stringify({
                    "id": __rid,
                    "time": time,
                    "TRelation": "during"
                }),
                // dataType: 'json',
                success: function (data) {
                    resolve(data)
                },
                error: function (error) {
                    reject(error)
                },
                timeout: 30000 // sets timeout to 30 seconds
            })
        })
        .catch(error => {
            _alertify("Error!!!", '<pre id="custom_alertbox">: ' + error["statusText"] + '</pre>');
            // console.log(error)
        })
    })
}

function _get_latest_data(_resource_id, _token) {
    //console.log(_token)
    $.ajax({
        "url": cat_conf['resoure_server_base_URL'] + "/search",
        "async": true,
        "processData": false,
        "crossDomain": true,
        "method": 'POST',
        "headers": { "token": _token, "Content-Type": "application/json" },
        "data": JSON.stringify({
            "id": _resource_id,
            "options": "latest"
        }),
        // dataType: 'json',
        success: function (data) {
            // alert("Success! \n"+data)
            // display_json_response_in_modal(data)
            _alertify("Success!!!", '<pre id="custom_alertbox">' + jsonPrettyHighlightToId(data) + '</pre>');
        },
        error: _alertify("Error!!!", '<pre id="custom_alertbox">: Please try some time later. Server is facing some problems at this moment.</pre>'),
        timeout: 30000 // sets timeout to 30 seconds
    })
}

function _alertify(header_msg, body_msg) {
    alertify.alert(body_msg);
    $(".ajs-header").html(header_msg);
}

function display_latest_data(e, ele, _rid) {
    e.preventDefault();   // use this to NOT go to href site
    _alertify("Getting Data...", get_spinner_html())
    __get_latest_data(cat_conf['resoure_server_base_URL'] + "/search", _rid)
        .then(data => {
            _alertify("Success!!!", '<pre id="custom_alertbox">' + jsonPrettyHighlightToId(data) + '</pre>')
        })
        .catch(error => {
            _alertify("Error!!!", '<pre id="custom_alertbox">: ' + error["statusText"] + '</pre>');
            console.log(error)
        })
}

function get_temporal_data_alert_html(){
    return `
        <input type="hidden" id="rid_in_hidden" name="rid_in_hidden" value="">
        <div class="form-group">
          <label for="data_keys">Select Y-Axis:</label>
          <select class="form-control" id="data_keys" onchange="update_temporal_data()">
          </select>
          <label for="duration">Select Duration:</label>
          <select class="form-control" id="duration" onchange="update_temporal_data()">
            <option value="7">1 Week</option>
            <option value="1">1 Day</option>
          </select>
        </div>
        <canvas id="custom_alertbox"></canvas>
    `
}

function update_temporal_data(){
    var _days = $( "#duration" ).val();
    var _rid = $('#rid_in_hidden').val()
    var __y_name = $( "#data_keys" ).val();
    __get_temporal_data(cat_conf['resoure_server_base_URL'] + "/search", _rid, _days)
        .then(data => {
            if(data.length == 0){
                // toast_alert("Data is empty", 'warning', '#1abc9c')
                alert("Data is empty")
            }else{
                var _x = []
                var _y = []
                var ctx = document.getElementById('custom_alertbox').getContext('2d');

                // get list of key in data
                var keys = Object.keys(data[0]);
                
                if(__y_name == undefined){
                    __y_name = keys[0]
                }
                for (var i = data.length - 1; i >= 0; i--) {
                    _x.push(formated_date(new Date(data[i]['LASTUPDATEDATETIME'])))
                    _y.push(parseInt(data[i][__y_name]));
                }

                window._chart = new Chart(ctx, get_conf('time', __y_name));
                window._chart.data.datasets=[]
                window._chart.data.datasets.push({
                        label: __y_name,
                        borderColor: 'red',
                        backgroundColor: 'rgba(0, 0, 0, 0)',
                        fill: false,
                        cubicInterpolationMode: 'monotone'
                })
                // console.log(_x,_y)
                window._chart.data.labels=_x.reverse();
                window._chart.data.datasets.forEach((dataset) => {
                    // console.log(dataset);
                    // console.log(_y)
                    dataset.data=_y.reverse();
                });
                window._chart.update();
            }
        })
        .catch(error => {
            _alertify("Error!!!", '<pre id="custom_alertbox">: ' + error["statusText"] + '</pre>');
            console.log(error)
        })
}

function get_week_day(__day_num){
    if(__day_num == 0){
        return "Sun"
    }else if(__day_num == 1){
        return "Mon"
    }else if(__day_num == 2){
        return "Tue"
    }else if(__day_num == 3){
        return "Wed"
    }else if(__day_num == 4){
        return "Thu"
    }else if(__day_num == 5){
        return "Fri"
    }else if(__day_num == 6){
        return "Sat"
    }
}

function formated_date(__date){
    return get_week_day(__date.getDay()) + " | " + __date.getHours() + ":" + __date.getMinutes();
}

function display_temporal_data(e, ele, _rid, __y_name) {
    e.preventDefault();   // use this to NOT go to href site
    _alertify("Getting Data...", get_spinner_html())
    __get_temporal_data(cat_conf['resoure_server_base_URL'] + "/search", _rid, 7)
        .then(data => {
            if(data.length == 0){
                _alertify("Success!!!", '<pre id="custom_alertbox">Data is empty</pre>')
            }else{
                _alertify("Success!!!", get_temporal_data_alert_html())
                $('#rid_in_hidden').val(_rid);
                var _x = []
                var _y = []
                var ctx = document.getElementById('custom_alertbox').getContext('2d');


                // get list of key in data
                var keys = Object.keys(data[0]);
                for (var i =  0; i < keys.length; i++) {
                    $('#data_keys').append(`<option value="`+keys[i]+`">`+keys[i]+`</option>`);
                }

                if(__y_name == undefined){
                    __y_name = keys[0]
                }

                for (var i = 0; i < data.length; i++) {
                    _x.push(formated_date(new Date(data[i]['LASTUPDATEDATETIME'])))
                    _y.push(parseInt(data[i][__y_name]));
                }

                window._chart = new Chart(ctx, get_conf('time', __y_name));
                window._chart.data.datasets=[]
                window._chart.data.datasets.push({
                        label: __y_name,
                        borderColor: 'red',
                        backgroundColor: 'rgba(0, 0, 0, 0)',
                        fill: false,
                        cubicInterpolationMode: 'monotone'
                })
                // console.log(_x,_y)
                window._chart.data.labels=_x.reverse();
                window._chart.data.datasets.forEach((dataset) => {
                    // console.log(dataset);
                    // console.log(_y)
                    dataset.data=_y.reverse();
                });
                window._chart.update();
            }
        })
        .catch(error => {
            _alertify("Error!!!", '<pre id="custom_alertbox">: ' + error["statusText"] + '</pre>');
            console.log(error)
        })
}

function get_filtered_url(__filter_url) {
    if (__filter_url == `attribute-name=("")&attribute-value=((""))`) {
        return ""
    } else {
        return "&" + __filter_url
    }
}

function toast_alert(__msg, __msg_type, __bg_color) {
    $.toast({
        text: `<b class="toast_msg">` + __msg + `</b>`,
        position: 'mid-center',
        hideAfter: 1800,
        loader: false,  // Whether to show loader or not. True by default
        loaderBg: '#1abc9c',
        bgColor: __bg_color,
        showHideTransition: 'fade', // fade, slide or plain
        allowToastClose: false, // Boolean value true or false
        icon: __msg_type // Type of toast icon  
    })
}

function reset_filter(__input_name) {
    $.each($(`input[name='` + __input_name + `']:checked`), function () {
        $(this).removeAttr("checked");
    });
    $('#ckbCheckAll').removeAttr("checked");
    var category = "";
    if (__input_name == "taglists") {
        category = "Tag"
    } else if (__input_name == "resource_server_group") {
        category = "Resource Server Group"
    } else if (__input_name == "provider") {
        category = "Provider"
    }
    console.log(get_selected_values_framed_url())
    var __filter_url = "/catalogue/v1/search?" + get_selected_values_framed_url()

    $.get(__filter_url, function (data, status) {
        markersLayer.clearLayers();
        data = JSON.parse(data)
        //console.log(data)
        for (var i = data.length - 1; i >= 0; i--) {
            // //console.log(data[i])
            if (data[i].hasOwnProperty('location')) {


                plotGeoJSONs(data[i]["location"]["value"]["geometry"], data[i]["id"], data[i], data[i]["resourceServerGroup"]["value"], data[i]["resourceId"]["value"]);
            } else if (data[i].hasOwnProperty('coverageRegion')) {


                plotGeoJSONs(data[i]["coverageRegion"]["value"]["geometry"], data[i]["id"], data[i], data[i]["resourceServerGroup"]["value"], data[i]["resourceId"]["value"]);
                //console.log("2")
            }
        }
    });

    toast_alert(category + ' filter has been cleared', 'success', '#1abc9c')
}

function round_off(__arr, __decimal_places) {
    var x = 0;
    var len = __arr.length
    while (x < len) {
        __arr[x] = __arr[x].toFixed(__decimal_places);
        x++
    }
    return __arr;
}

function toast_alert_for_response_data_length(__data) {
    var len = __data.length;
    if (len == 0) {
        toast_alert('Zero items found for this query', 'warning', '#c0392b');
    } else {
        toast_alert('Found ' + len + ' items for this query', 'success', '#1abc9c');
    }
}

function showDetails() {
    //console.log("print this...")
    $('#_batch').hide();
    $('#point').show();
}

function get_selected_values_framed_url() {
    var value = get_selected_values_checkbox();
    var tags = value.tags;
    var rsg = value.rsg;
    var provider = value.provider;
    //console.log(tags, rsg , provider)

    var __filter_url = ""

    if (tags.length == 0 && rsg.length == 0 && provider.length == 0) {
        __filter_url = `attribute-name=("")&attribute-value=((""))`
    } else {
        // //console.log("else...")
        var _attr_names = get_api_encoded_attribute_names(tags, rsg, provider)
        // //console.log(_attr_names)
        var _attr_values = get_api_encoded_attribute_values(tags, rsg, provider)
        // //console.log(_attr_values)
        __filter_url = `attribute-name=` + _attr_names + `&attribute-value=` + _attr_values + get_geo_shape_url(geo_shape)
    }
    return __filter_url;
}

function get_geo_shape_url(__geo_shape) {
    var _url = ""
    if (__geo_shape != null) {
        if (__geo_shape['type'] == 'circle') {
            _url = "&lat=" + __geo_shape.value.center_point["lat"] + "&lon=" + __geo_shape.value.center_point["lng"] + "&radius=" + __geo_shape.value.radius
        } else if (__geo_shape['type'] == 'marker') {
            _url = "&lat=" + __geo_shape.value.center_point["lat"] + "&lon=" + __geo_shape.value.center_point["lng"]
        } else if (__geo_shape['type'] == 'polygon') {
            _url = "&geometry=polygon((" + __geo_shape.value.points + "," + __geo_shape.value.points[0] + "))&relation=within"
        } else if (__geo_shape['type'] == 'rectangle') {
            _url = "&bbox=" + __geo_shape.value.bbox_points + "&relation=within"
        } else if (__geo_shape['type'] == 'polyline') {
            _url = "&bbox=" + __geo_shape.value.bbox_points + "&relation=within"
        }
    }

    return _url;
}

function _get_security_based_latest_data_link(_resource_id, _resourceServerGroup, _rid, token) {
    // if(_resource_id=="rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/safetipin/safetipin/safetyIndex"){
    return `<button class="btn btn-secondary" onclick="_get_latest_data('` + _resource_id + `','` + token + `')">Get Full Latest Data</button>`
    // }else{
    //     return `<a href="#" class="data-modal"  onclick="display_latest_data(event, this, '`+_resource_id+`')">Get Latest Data</a>`
    // }
}

function request_access_token(resource_id, resourceServerGroup, rid) {
    //console.log(resource_id)
    $.ajax({
        url: cat_conf['auth_base_URL'] + "/token",
        type: 'post',
        dataType: 'json',
        contentType: 'application/json',
        data: JSON.stringify({ "request": { "resource-id": resource_id } }),
        success: function (data) {
            // //console.log(data.token)

            // $('#token_section_'+resource_id_to_html_id(resource_id)).html($('#token_section_'+resource_id_to_html_id(resource_id)).html());
            $('#token_section_' + resource_id_to_html_id(resource_id)).html(
                `<b>Token</b>: <span id="token_value_` + resource_id_to_html_id(resource_id) + `">` + data.token + `</span>`
                + `&nbsp;&nbsp;&nbsp;<button class="btn copy-btn" onclick="copyToClipboard('` + resource_id_to_html_id(resource_id) + `')"> Copy Token <img class="secure_icon svg-white" src="../assets/img/icons/copy_white.svg"></button> <br> `
                + _get_security_based_latest_data_link(resource_id, resourceServerGroup, rid, data.token))

            _alertify("Success!!!", "Token received.<br>You are now authenticated to access the non-public data.")
            // _alertify("Success!!!", "Token received: " + data.token)
            if (!($('#token_section_' + resource_id_to_html_id(resource_id)).is(':visible'))) {
                $('#token_section_' + resource_id_to_html_id(resource_id)).toggle();
            }

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
    json = json.replace(/("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?)/g, function (match) {
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
        // ////console.log(cls, match)
        return '<span style="' + cls + '">' + urlify(match) + '</span>';
    });
    // return urlify(json);
    return json;
}

function jsonPrettyHighlightToIdwithBR(jsonobj) {

    var json = JSON.stringify(jsonobj, undefined, 2);

    json = json.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    // //console.log(json.replace(/\n/g, "<br />"))
    json = json.replace(/\n/g, "<br />")
    json = json.replace(/("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?)/g, function (match) {
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
        // ////console.log(cls, match)
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
    return new Promise(function (resolve, reject) {
        setTimeout(function () {
            sidebar.show();
        }, time_milli_seconds);
    });
}

function onEachFeature(feature, layer) {
    layer.bindPopup(feature.properties.name);
}

function getColorsForPolygon(_resourceServerGroup) {

    // var colors=["#1abc9c", '#f1c40f']//, '#9b59b6']//, '#e67e22', '#f39c12']
    var colors = ['#1abc9c', '#f1c40f', '#FF0000', '#ffffff00'];

    if (_resourceServerGroup == "crowd-sourced-changebhai" || _resourceServerGroup == "changebhai") {
        // loop through our density intervals and generate a label with a colored square for each interval
        //console.log("changeBhai")
        // div.innerHTML +=  
        // '<span style="background-color:' + colors[0] + '"></span> ' +
        //   'ChangeBhai' + '<br>';
        // console.log("changebhai")
        return colors[0];
    } else if (_resourceServerGroup == "safetipin") {
        // div.innerHTML +=  
        // '<span style="background-color:' + colors[1] + '"></span> ' +
        //   'safetiPin' + '<br>';
        // console.log("safetipin")
        return colors[1]

    } else if (_resourceServerGroup == "traffic-incidents" || _resourceServerGroup == "tomtom") {
        // div.innerHTML +=  
        // '<span style="background-color:' + colors[2] + '"></span> ' +
        //   'TomTom' + '<br>';
        // console.log("tomtom")
        return colors[2]
    } else if (_resourceServerGroup == "itms-mobility") {
        //   console.log("itms-mobility")
        return colors[3]
    }


}


// var color_count=-1;

// function getRandomColor(){
//  var color =  "#" + (Math.random() * 0xFFFFFF << 0).toString(16);
//  return color;
// }data[i]["id"]
// function getColorForPolygon()
function plotGeoJSONs(geoJSONObject, _id, _json_object, _resourceServerGroup, _resourceId, _tags, _provider) {
    ////console.log(_resourceServerGroup)
    // ////console.log("plotting "+ geoJSONObject, _id, _id["id"])
    //console.log(geoJSONObject, _id, _json_object, _resourceServerGroup, _resourceId ,_tags , _provider)
    // _provider_data = _provider;
    // _tags_data = _tags;
    // _resourceServerGroup_data =_resourceServerGroup;
    // _geoJSONObject = geoJSONObject;
    // _resourceId_data = _resourceId;

    ////console.log(geoJSONObject)


    if (geoJSONObject["type"] == "Polygon") {

        //console.log("Printing Polygon....")
        // color_count=color_count+1
        // var _color=getRandomColor()

        var div = $('div.info.legend');

        //console.log(_resourceServerGroup, div)
        var is_public = (_json_object['secure']||[]).length === 0;
        var is_secure = (_json_object['secure']||[]).length !== 0;;


        L.geoJSON(geoJSONObject, {
            style: {
                // fillColor: colors[color_count],
                // fillColor: _color,
                fillColor: getColorsForPolygon(_resourceServerGroup),
                weight: 2,
                opacity: 1,
                // color: 'white',
                // dashArray: '3',
                fillOpacity: 0.5
            },
            onEachFeature: function (feature, layer) {
                layer.on('click', function (e) {

                    show_details(_id)

                });
                layer.bindPopup(`<span class="float-left" style="padding-right:7.5px;"><img src='`+
                ((is_public) ? "../assets/img/icons/green_unlock.svg" : "../assets/img/icons/red_lock.svg")
                +`' class='img-fluid secure_icon'></span><a href='#' class='data-modal'  onclick="display_latest_data(event, this, '` + _id + `')">Get latest-data</a><br>`
                +`<a href="#" class="data-modal" onclick="display_temporal_data(event, this, '`+_json_object.id+`')">Get Temporal Data</a><br>`+
                ((is_secure) ? `<a href='#' class='data-modal'  onclick="request_access_token('` + _json_object.id + `', '`+ _json_object["resourceServerGroup"]["value"] + `', '`+ _json_object["resourceId"]["value"] + `')">Request Access Token</a>` : ``)
                ).addTo(map);
            }

        }).addTo(markersLayer);

    }
    else if (geoJSONObject["type"] == "Point") {

        var is_public = (_json_object['secure']||[]).length === 0;
        // //console.log("Printing Point....")
        L.geoJSON(geoJSONObject, {
            pointToLayer: function (feature, latlng) {
                // console.log(_resourceServerGroup)
                // return L.marker(latlng, {icon: getOfficeIcon()});

                // <a href='/catalogue/v1/items/"+plot_id+"'>Get Catalogue-item-details</a><br/>
                var customPopup = `<span class="float-left" style="padding-right:7.5px;"><img src='`+
                ((is_public) ? "../assets/img/icons/green_unlock.svg" : "../assets/img/icons/red_lock.svg")
                +`' class='img-fluid secure_icon'></span><a href='#' class='data-modal'  onclick="display_latest_data(event, this, '` + _id + `')">Get latest-data</a>`
                +`<br><a href="#"  class="data-modal" onclick="display_temporal_data(event, this, '`+_json_object.id+`')">Get Temporal Data</a><br>`;
                var _marker = L.marker(latlng, { icon: getMarkerIcon(_resourceServerGroup) }).addTo(map);
                _marker.itemUUID = _id;
                // console.log(_id,this,event)
                //////console.log(_marker.itemUUID); _marker.bindPopup(customPopup) _marker.bindPopup(customPopup)
                _marker.on('click', markerOnClick);
                _marker.bindPopup(customPopup)
                return _marker;
            },
            // filter: filter_byTags,
            // onEachFeature: onEachFeature
        }).addTo(markersLayer);
        // //console.log("22222222222")
    }
}


function get_latest_data_url() {
    return cat_conf['resoure_server_base_URL'] + `/search`
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
    ////console.log("batch mode")
    hide_menu_icon();
}

function activate_point_mode(_id) {
    // //console.log(1,_id)
    $("#_batch").hide();
    // //console.log(2,_id)
    // //console.log("called")
    show_details(_id)
    $("#point").show();
}

function resource_id_to_html_id(resource_id) {
    var replace_with = "_";
    return resource_id.replace(/\/|\.|\s|\(|\)|\<|\>|\{|\}|\,|\"|\'|\`|\*|\;|\+|\!|\#|\%|\^|\&|\=|\₹|\$|\@/g, replace_with)
}

function markerOnClick(e) {
    // var attributes = e.layer.properties;
    //////console.log(e.target.itemUUID)

    activate_point_mode(e.target.itemUUID);
    sidebar.show();
    // alert(e.target.itemUUID);
    // ////console.log(attributes);
    // do some stuff…
}


function getMapDefaultViewLatLng() {
    return cat_conf['map_default_view_lat_lng']
}

function getMarkerIconOptions(__rsg) {
    return {
        iconUrl: legends[__rsg],
        // shadowUrl: 'leaf-shadow.png',

        iconSize: [38, 95], // size of the icon
        iconAnchor: [12, 41], // point of the icon which will correspond to marker's location
        popupAnchor: [1, -34], // point from which the popup should open relative to the iconAnchor
        shadowSize: [41, 41]  // size of the shadow
    }
}

function getOfficeIcon() {
    var officeIcon = L.icon({
        iconUrl: 'https://image.flaticon.com/icons/svg/167/167707.svg',
        // shadowUrl: 'leaf-shadow.png',

        iconSize: [38, 95], // size of the icon
        shadowSize: [50, 64], // size of the shadow
        iconAnchor: [22, 94], // point of the icon which will correspond to marker's location
        shadowAnchor: [4, 62],  // the same for the shadow
        popupAnchor: [-3, -76] // point from which the popup should open relative to the iconAnchor
    });
    return officeIcon;
}


function getMarkerIcon(__rsg) {
    return L.icon(getMarkerIconOptions(__rsg));
}


function get_selected_values_checkbox() {
    var _tags = [];
    var _rsg = [];
    var _pr = [];

    $.each($("input[name='taglists']:checked"), function () {
        _tags.push($(this).val());
    });  

    $.each($("input[name='resource_server_group']:checked"), function () {
        _rsg.push($(this).val());
    });

    $.each($("input[name='provider']:checked"), function () {
        _pr.push($(this).val());
    });
    //alert("My taglists are: " + _tags.join(", ")+"and My resource group are:" +_rsg.join(", "));

    return values =
        {
            "tags": _tags,
            "rsg": _rsg,
            "provider": _pr
        }

}

$(document).ready(function () {
    $("#smartcity_name").html(cat_conf['smart_city_name'] + " IUDX | Indian Urban Data Exchange Catalogue ")
    $("#smart_city_link").html(cat_conf['smart_city_name'])
    $("#smart_city_link").attr('href', cat_conf['smart_city_url'])
    $("#smart_iudx_city_logo").attr('src', cat_conf['smart_city_iudx_logo'])
});