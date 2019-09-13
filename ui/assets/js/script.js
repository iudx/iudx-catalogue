var DATA;

//disable zoomControl when initializing map (which is topleft by default)
var map = L.map('map', {
    zoomControl: false
    //... other options
});

map.setView(getPuneLatLng(), 12);
//add zoom control with your options
L.control.zoom({
    position: 'topright'
}).addTo(map);

// L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
//     attribution: '© <a href="http://openstreetmap.org/copyright">OpenStreetMap</a> contributors',
//     maxZoom: 100
// }).addTo(map);

var tile_layer = L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
    attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors © <a href="https://carto.com/attributions">CARTO</a>',
    subdomains: 'abcd',
    maxZoom: 100
});

tile_layer.addTo(map);

L.Control.Watermark = L.Control.extend({
    onAdd: function(map) {
        var img = L.DomUtil.create('img');
        
        img.src = '../assets/img/iudx_pscdcl.png';
        img.style.width = '250px';
        
        return img;
    },
    
    onRemove: function(map) {
        // Nothing to do here
    }
});

L.control.watermark = function(opts) {
    return new L.Control.Watermark(opts);
}

L.control.watermark({ position: 'bottomright' }).addTo(map);

//Adding Legend to the map

var legend = L.control({position: 'bottomright'});

legend.onAdd = function (map) {

    var div = L.DomUtil.create('div', 'info legend'),
        grades = ["StreetLight", "AQM","Flood-Sensor","Wifi-Hotspot","ITMS","ChangeBhai","SafetyPin","TomTom"],
        labels = ["https://img.icons8.com/color/48/000000/street-light.png","https://img.icons8.com/color/48/000000/air-quality.png","https://img.icons8.com/office/16/000000/sensor.png","https://img.icons8.com/flat_round/64/000000/wi-fi-connected.png","https://img.icons8.com/ultraviolet/40/000000/marker.png","https://img.icons8.com/color/48/000000/marker.png","https://img.icons8.com/flat_round/64/000000/safety-pin--v2.png","https://image.flaticon.com/icons/svg/1167/1167993.svg"];

    // loop through our density intervals and generate a label with a colored square for each interval
    var heading ='<h4>LEGENDS</h4>'
    for (var i = 0; i < grades.length; i++) {
        div.innerHTML +=  
            (" <img src="+ labels[i] +" height='50' width='50'>") +grades[i] +'<br>';
    }
    div.innerHTML = heading+div.innerHTML;
    return div ;
};

legend.addTo(map);
// create the sidebar instance and add it to the map

var sidebar = L.control.sidebar('sidebar', {
    closeButton: true,
    position: 'left'
});

map.addControl(sidebar);

var marker = L.marker(getPuneLatLng(), {icon: getOfficeIcon()}).addTo(map).on('click', function (event) {
    sidebar.show(1000);
});

marker.itemUUID = "Pune Office"
//Create a marker layer 
var markersLayer = L.featureGroup().addTo(map);
marker.on("click", markerOnClick);
//Add markers to marker layer
// marker.addTo(markersLayer);



markersLayer.on("click", markerOnClick);

$("#menu-bar-icon").click(function (e) {
    sidebar.toggle();
});


sidebar.on('show', function () {
    hide_menu_icon();
    // console.log('Sidebar will be visible.');
});

sidebar.on('shown', function () {
    // console.log('Sidebar is visible.');
});

sidebar.on('hide', function () {
    show_menu_icon();
    // console.log('Sidebar will be hidden.');
});

sidebar.on('hidden', function () {
    // console.log('Sidebar is hidden.');
});

L.DomEvent.on(sidebar.getCloseButton(), 'click', function () {
    // console.log('Close button clicked.');
    show_menu_icon();
});



// async function populate_side_bar() {

// }

var drawnItems = new L.FeatureGroup();
map.addLayer(drawnItems);

var drawPluginOptions = {
    position: 'topright',
    draw: {
        // disable toolbar item by setting it to false
        polygon: false,
        polyline: false,
        rectangle: false,
        marker: true,
    },
    edit: {
        featureGroup: drawnItems, //REQUIRED!!
        remove: true
    }
    
};
L.drawLocal.draw.toolbar.buttons.circle = 'Draw a circle!';

var drawControl = new L.Control.Draw(drawPluginOptions);
map.addControl(drawControl);
// new L.Illustrate.Control({
//     edit: { featureGroup: drawnItems }
// }).addTo(map);

map.on('draw:created', async function (e) {
    drawnItems.clearLayers();
    // settimeout(1000);
    var type = e.layerType,
    
        layer = e.layer;

    //drawnItems.addLayer(layer);
    if (type === 'circle') {
        var center_point = layer.getLatLng();
        var radius = layer.getRadius();
        // console.log(radius)
        markersLayer.clearLayers();

        console.log("/catalogue/v1/search?lat="+ center_point["lat"] +"&lon="+ center_point["lng"] +"&radius="+radius)

		$.get("/catalogue/v1/search?lat="+ center_point["lat"] +"&lon="+ center_point["lng"] +"&radius="+radius, function(data) {
		//$.get("/catalogue/v1/search?lat=12.273737&lon=78.37475&radius=200000", function(data) {
        //$.get("/search/catalogue/attribute?bounding-type=circle&lat="+ center_point["lat"] +"&long="+ center_point["lng"] +"&radius="+radius, function(data) {
        	
            data=JSON.parse(data);
            console.log(data.length)
            for (var i = data.length - 1; i >= 0; i--) {
                // console.log(data[i])
                           if(data[i].hasOwnProperty('location')){
                    // myLayer.addData(data[i]['geoJsonLocation']);
            plotGeoJSONs(data[i]["location"]["value"]["geometry"], data[i]["id"],data[i]["id"],data[i]["resourceServerGroup"]["value"],data[i]["resourceId"]["value"]);
                }else if(data[i].hasOwnProperty('coverageRegion')){
                    // myLayer.addData(data[i]['geoJsonLocation']);
                    console.log("1")
            plotGeoJSONs(data[i]["coverageRegion"]["value"]["geometry"], data[i]["id"],data[i]["id"],data[i]["resourceServerGroup"]["value"],data[i]["resourceId"]["value"]);
            console.log("2")        
        }
            }
		// console.log(data.length)
            // DATA=data
        });
        // await geoquery_circle(center_point["lat"],center_point["lng"], radius)
    }

    if (type === 'marker') {
        var center_point = layer.getLatLng();
        //var radius = layer.getRadius();
        console.log(layer.getLatLng());
        // console.log(radius);
        markersLayer.clearLayers();

		$.get("/catalogue/v1/search?lat="+ center_point["lat"] +"&lon="+ center_point["lng"] +"&radius=100", function(data) {
		//$.get("/catalogue/v1/search?lat=12.273737&lon=78.37475&radius=200000", function(data) {
        //$.get("/search/catalogue/attribute?bounding-type=circle&lat="+ center_point["lat"] +"&long="+ center_point["lng"] +"&radius="+radius, function(data) {
        	
            data=JSON.parse(data);
            console.log(data)
            for (var i = data.length - 1; i >= 0; i--) {
                console.log(Rohinarrr)
                if(data[i].hasOwnProperty('location')){
                    // myLayer.addData(data[i]['geoJsonLocation']);
		    plotGeoJSONs(data[i]["location"]["value"]["geometry"], data[i]["id"],data[i]["id"],data[i]["resourceServerGroup"]["value"],data[i]["resourceId"]["value"]);
                }
            }
		// console.log(data.length)
            // DATA=data
        });
       
    }

    if (type === 'polygon') {
        // here you got the polygon points
        var points = layer._latlngs;

        // here you can get it in geojson format
        var geojson = layer.toGeoJSON();
        // console.log(points, geojson);
    }
    //    if (type === 'polygon') {
    //         // structure the geojson object
    //         var geojson = {};
    //         geojson['type'] = 'Feature';
    //         geojson['geometry'] = {};
    //         geojson['geometry']['type'] = "Polygon";

    //         // export the coordinates from the layer
    //         coordinates = [];
    //         latlngs = layer.getLatLngs();
    //         for (var i = 0; i < latlngs.length; i++) {
    //             coordinates.push([latlngs[i].lng, latlngs[i].lat])
    //         }

    //         // push the coordinates to the json geometry
    //         geojson['geometry']['coordinates'] = [coordinates];

    //         // Finally, show the poly as a geojson object in the console
    //         console.log(JSON.stringify(geojson));

    //     }

    drawnItems.addLayer(layer);
});

// layer.on('click', function(e){
//     console.log("Polygon data: " + e.target.getUserData() + " Points: " + e.target.getLatLngs());
// });
map.on('click', function (e) {
    $('#filter').removeAttr('disabled');
    //console.log('click',e.latlng);
    var lat_lng_arr = [];
    var lat_lng_arr1 = [];
    lat_lng_arr.push(e.latlng['lat']);
    lat_lng_arr.push(e.latlng['lng']);

    // console.log(lat_lng_arr);
});

// map.on('touchstart', function(){
//  window.console.log('touchstart');
// });


// var myLayer = L.geoJSON().addTo(map);

//ajax call to get resource-items from /list/catalogue/resource-item

function display_swagger_ui(_openapi_url){
    $("#map").hide();
    $("#swagger_section").fadeIn(1500);
    const ui = SwaggerUIBundle({
    //url: "https://petstore.swagger.io/v2/swagger.json",
    url: _openapi_url,
    dom_id: '#swagger_ui',
    deepLinking: true,
    presets: [
      SwaggerUIBundle.presets.apis,
      SwaggerUIStandalonePreset
    ],
    plugins: [
      SwaggerUIBundle.plugins.DownloadUrl
    ],
    layout: "StandaloneLayout"
    })
}


function display_map(){
    $("#swagger_section").hide();
    $("#map").fadeIn(1500);
}

function show_details(_id){
    $.get("/catalogue/v1/items/" + _id , function(data) {
        data=JSON.parse(data)
        // console.log(data)
        // console.log(data[0]["resourceId"]["value"])
        // console.log(data[0]["itemDescription"])
        // console.log(data[0]["itemType"]["value"])
        // console.log(data[0]["provider"]["value"])
        // console.log(data[0]["createdAt"]["value"])
        // console.log(data[0]["resourceServerGroup"]["value"])
        // console.log(data[0]["itemStatus"]["value"])
        // console.log(data[0]["refBaseSchema"]["value"])
        // console.log(data[0]["refDataModel"]["value"])

        var id = resource_id_to_html_id(_id)
        //console.log(id);
        
        $("#resource_item_details").html(`
            <table class="table table-borderless table-dark">
              <thead>
                <tr></tr>
              </thead>
              <tbody id="_tbody">
            <tr>
                  <th scope="row">Resource-Id</th>
                  <td>`+ data[0]["resourceId"]["value"] +`</td>
            </tr>
            <tr>
                  <th scope="row">Description</th>
                  <td>`+ data[0]["itemDescription"] +`</td>
            </tr>
            <tr>
                  <th scope="row">Type</th>
                  <td>`+ data[0]["itemType"]["value"] +`</td>
            </tr>
            <tr>
                  <th scope="row">Provider</th>
                  <td>`+ data[0]["provider"]["value"] +`</td>
            </tr>
            <tr>
                  <th scope="row">Created-On</th>
                  <td>`+ data[0]["createdAt"]["value"] +`</td>
            </tr>
            <tr>
                  <th scope="row">Resource Server Group</th>
                  <td>`+ data[0]["resourceServerGroup"]["value"] +`</td>
            </tr>
           
            <tr>
                  <th scope="row">Status</th>
                  <td>`+ data[0]["itemStatus"]["value"] +`</td>
            </tr>
            </tbody>
            </table>
        `);
        $("#resource_item_special_feature_links").html(`
                    <button onclick="display_swagger_ui('` + data[0]["accessInformation"]["value"][0]["accessObject"]["value"] + `')">API Details</button>
                    <button><a href="`+data[0]["refDataModel"]["value"]+`" target="_blank">Data Model </a></button>
            `);
    });
}


$( document ).ready(function() {
    $.get("/catalogue/v1/search", function(data, status){
   // $.get("/list/catalogue/resource-item", function(data, status){
         //console.log("Data: " + data + "\nStatus: " + status);
//console.log("Rohina");
        data=JSON.parse(data)
        for (var i = data.length - 1; i >= 0; i--) {
            // console.log(data[i])
            // console.log(data[i]["location"]["value"]["geometry"])
            // if(data[i].hasOwnProperty('location')){
            //     // myLayer.addData(data[i]['geoJsonLocation']);
            //     plotGeoJSONs(data[i]["location"]["value"]["geometry"], data[i]["id"],data[i]["id"],data[i]["resourceServerGroup"]["value"],data[i]["resourceId"]["value"]);
            // }
            if(data[i].hasOwnProperty('location')){
                    // myLayer.addData(data[i]['geoJsonLocation']);
            plotGeoJSONs(data[i]["location"]["value"]["geometry"], data[i]["id"],data[i]["id"],data[i]["resourceServerGroup"]["value"],data[i]["resourceId"]["value"]);
                }else if(data[i].hasOwnProperty('coverageRegion')){
                    // myLayer.addData(data[i]['geoJsonLocation']);
                    console.log("1")
            plotGeoJSONs(data[i]["coverageRegion"]["value"]["geometry"], data[i]["id"],data[i]["id"],data[i]["resourceServerGroup"]["value"],data[i]["resourceId"]["value"]);
            console.log("2")   
        }
    }
    });
});

