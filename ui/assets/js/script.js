/******************************** Public view functions ************************************/

    // Disable ctrl+s to prevent downloading source code 
    $(document).bind('keydown', function(e) {
        if (e.ctrlKey && (e.which == 83)) {
            e.preventDefault();
            // alert('Ctrl+S');
            return false;
        }
    });

    var sensorIcon = L.icon({
    iconUrl: 'assets/img/icons/1481951.svg',

    iconSize:     [38, 95], // size of the icon
    shadowSize:   [50, 64], // size of the shadow
    iconAnchor:   [22, 94], // point of the icon which will correspond to marker's location
    shadowAnchor: [4, 62],  // the same for the shadow
    popupAnchor:  [-3, -76] // point from which the popup should open relative to the iconAnchor
    });

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


    function getUniqueResourceClass(_data){
        return $.unique(JSON.parse(_data).map(function (d) {
                    return d.accessInformation[0].accessVariables.resourceClass;
                }));
    }


    function performLocalStorage(_data){
        localStorage.setItem("data", _data);
        var rc = getUniqueResourceClass(_data);
        localStorage.setItem("resourceClass", rc);
        localStorage.setItem("resourceClassColors", randomColor({'luminosity':'dark', 'count': rc.length}));
    }

    function cache_cat() {
          return new Promise(function(resolve, reject) {
            // resourceClass =  $.unique(data.map(function (d) {
            //         return d.accessInformation[0].accessVariables.resourceClass
            //     }));
             $.get("list/catalogue/catalogue-item", function(data) {
                performLocalStorage(data)
                resolve(data);
            });
          });
    }

    // function display_tag_cloud() {
    //     var seen_tags_set = [];
    //     var tag_cloud_html = [];
    //     data = JSON.parse(localStorage.getItem("data"))
    //     for (var item_index = 0; item_index < data.length - 1; item_index++) {
    //         for (var tag_item_index = 0; tag_item_index < data[item_index]['tags'].length - 1; tag_item_index++) {
    //             if (!seen_tags_set.includes(data[item_index]['tags'][tag_item_index])) {
    //                 seen_tags_set.push(data[item_index]['tags'][tag_item_index])
    //                 tag_cloud_html.push(`<a href="/search/catalogue/attribute?attribute-name=(tags)&attribute-value=((` + data[item_index]['tags'][tag_item_index] + `)" target="_blank"><button style="margin:10px;" type="button" class="btn btn-default">` + data[item_index]['tags'][tag_item_index] + `</button></a>`)
    //             }
    //         }
    //     }
    //     seen_tags_set = []
    //     $("#tag_cloud").html(tag_cloud_html);
    //     tag_cloud_html = []
    // }

    // display_tag_cloud();

    async function populate_side_bar() {
        var seen_tags_set = [];
        var tag_cloud_html = [];
        if (localStorage.getItem("data") == null) {
            await cache_cat();
        }
        data = JSON.parse(localStorage.getItem("data"))
        $("#item_result_count").html("Total number of items: " + data.length);
        for (var item_index = 0; item_index < data.length - 1; item_index++) {
            for (var tag_item_index = 0; tag_item_index < data[item_index]['tags'].length - 1; tag_item_index++) {
                // if(data[item_index]['tags'][tag_item_index].toLowerCase()=="feeder" || data[item_index]['tags'][tag_item_index].toLowerCase()=="streetlight" || data[item_index]['tags'][tag_item_index].toLowerCase()=="streetlighting"){
                //     continue;
                // }
                if (!seen_tags_set.includes(data[item_index]['tags'][tag_item_index].toLowerCase())) {
                    seen_tags_set.push(data[item_index]['tags'][tag_item_index].toLowerCase())
                }
            }
        }

        for (var i = seen_tags_set.length - 1; i >= 0; i--) {

            //For sidebar
            // tag_cloud_html.push(`<li class="nav-item nav-profile" onclick="show_items_of(this,'` + seen_tags_set[i] + `','`+i+`')">
            tag_cloud_html.push(`<li class="nav-item nav-profile" onclick="show_items_of(this)">
           
            <a class="nav-link" href="#">` + seen_tags_set[i] + ` &nbsp;
              <span id="tag_item_count-`+i+`" class="tag_item_count"></span>
            </a>
          </li>`)
        }

        $("#tags_side_bar").html(tag_cloud_html.sort());
        // $("#tags_side_bar").html(tag_cloud_html);
        tag_cloud_html = []

        // show_all_items();

        //For search auto complete
        return seen_tags_set;
    }

    var options = {
        collapsed: true,
        rootCollapsable: true,
        withQuotes: true,
        withLinks: true
    };

    var layerGroup;

    function getIconOptions(color){
        return {
                    icon: 'plane'
                        , borderColor: color
                        , textColor: color
                        , backgroundColor: color
                };
    }

    function getIndexOf(_resourceClass){
        return localStorage.getItem('resourceClass').split(',').indexOf(_resourceClass);
    }

    function getColorOf(_index){
        return localStorage.getItem('resourceClassColors').split(',')[_index];
    }

    function plot_point_on_map(lat_long, message){
        // L.marker(lat_long,{icon: sensorIcon}).addTo(map).bindPopup(message).openPopup();
        var marker = L.marker(lat_long,{icon: sensorIcon}).addTo(map).bindPopup(message);
    }

    function plot_point_on_map(lat_long, message, i){
        // L.marker(lat_long,{icon: sensorIcon}).addTo(map).bindPopup(message).openPopup();
        var marker = L.marker(lat_long,{
            icon: L.BeautifyIcon.icon(getIconOptions(getColorOf(i)))
        }).addTo(layerGroup).bindPopup(message);
    }

     function plot_points_on_map(data){
        layerGroup.clearLayers();
        for (var i =  0; i < data.length; i++) {
                plot_point_on_map([data[i]['geoJsonLocation']['coordinates'][1],data[i]['geoJsonLocation']['coordinates'][0]],`<h5 class="mr-2 mb-0 text-info"><a href="`+data[i]['latestResourceData']+`" target="_blank">` + data[i]['NAME'] + `</a></h5>
                      <p class="mb-0 font-weight-light">` + data[i]['itemDescription'] + `</p><br><label class="badge" style="background-color:`+getColorOf(getIndexOf(data[i]['accessInformation'][0]['accessVariables']['resourceClass']))+`">` + data[i]['accessInformation'][0]['accessVariables']['resourceClass'] + `</label>`, getIndexOf(data[i]['accessInformation'][0]['accessVariables']['resourceClass']))
            }
    }

    //For singular tag selection (like a radio button)
    function show_items_of(e, tag, index) {
        $(".nav-item").removeClass("_active");
        $(e).addClass("_active");
         $(".tag_item_count").html("");
        $.get("/search/catalogue/attribute?attribute-name=(tags)&attribute-value=((" + tag + "))", function(data) {
            // console.log(data)
            data = JSON.parse(data)
            var html_to_add = "";
            var item_details_card_html = ""
            $("#tag_item_count-"+index).html("("+data.length+")");
            for (var i = data.length - 1; i >= 0; i--) {
                html_to_add += 
  `            <div class="col-md-12 grid-margin stretch-card">
              <div class="card">
                <div class="card-body">
                  <div class="d-flex align-items-top mb-2">
                    <img src="https://image.flaticon.com/icons/svg/1481/1481951.svg" class="img-sm rounded-circle mr-3" alt="image">
                    <div class="mb-0 flex-grow">
                      <h5 class="mr-2 mb-0 text-info">` + data[i]['NAME'] + `</h5>
                      <p class="mb-0 font-weight-light">` + data[i]['itemDescription'] + `</p>
                    </div>
                     <button type="button" class="btn btn-outline-success btn-fw btn-detail" onclick="show_details_of(this,'` + i + `')">Details</button>
                    <!-- &nbsp;<button type="button" class="btn btn-outline-info btn-fw" onclick="show_latest_data('` + i + `','` + data[i]['latestResourceData'] + `')">Latest Data</button> -->
                  </div>
                  
                   <pre id="json-renderer-` + i + `" class="id_row"  style="height: 450px; display:none;">` + jsonPrettyHighlightToId(data[i]) + `</pre>
                  
                  <!--<iframe id="iframe-` + i + `" width="100%" height="100%" src="` + data[i]['latestResourceData'] + `" style="display:none">
                      <p>Your browser does not support iframes.</p> -->
                  </iframe>
                </div>
              </div>
            </div>`


            }
            $("#searched_items").html(html_to_add)
            $("#item_details_card").html(item_details_card_html)
        });
    }

    //For multi tag selection (like a checkbox)
    function show_items_of(e) {
        // $(".nav-item").removeClass("_active");
        if($(e).hasClass("_active")){
            $(e).removeClass("_active");
        }else{
            $(e).addClass("_active");    
        }
        var tags="";
        $("._active").each(function() {
            tags += $(this).find(".nav-link").html().split(" &nbsp;")[0];
            tags +=",";
        });
        // console.log(tags.slice(0, -1))
         $(".tag_item_count").html("");
        $.get("/search/catalogue/attribute?attribute-name=(tags)&attribute-value=((" + tags.slice(0, -1) + "))", function(data) {
            // console.log(data)
            data = JSON.parse(data)

            if($(".__active").attr('id')=='list_view'){
            var html_to_add = "";
            var item_details_card_html = ""
            $("#retrieved_item_count").html("&nbsp;| &nbsp;Items retrieved : <span style='color:red'>"+data.length+"</span>");
            for (var i = data.length - 1; i >= 0; i--) {
                html_to_add += 
  `            <div class="col-md-12 grid-margin stretch-card">
              <div class="card">
                <div class="card-body">
                  <div class="d-flex align-items-top mb-2">
                    <img src="https://image.flaticon.com/icons/svg/1481/1481951.svg" class="img-sm rounded-circle mr-3" alt="image">
                    <div class="mb-0 flex-grow">
                      <h5 class="mr-2 mb-0 text-info">` + data[i]['NAME'] + `</h5>
                      <p class="mb-0 font-weight-light">` + data[i]['itemDescription'] + ` &nbsp;&nbsp;&nbsp;<br><label class="badge"  style="background-color:`+getColorOf(getIndexOf(data[i]['accessInformation'][0]['accessVariables']['resourceClass']))+`">` + data[i]['accessInformation'][0]['accessVariables']['resourceClass'] + `</label></p>
                    </div>
                     <button type="button" class="btn btn-outline-success btn-fw btn-detail" onclick="show_details_of(this,'` + i + `')">Details</button>
                    <!-- &nbsp;<button type="button" class="btn btn-outline-info btn-fw" onclick="show_latest_data('` + i + `','` + data[i]['latestResourceData'] + `')">Latest Data</button> -->
                  </div>
                  
                   <pre id="json-renderer-` + i + `" class="id_row"  style="height: 450px; display:none;">` + jsonPrettyHighlightToId(data[i]) + `</pre>
                  
                  <!--<iframe id="iframe-` + i + `" width="100%" height="100%" src="` + data[i]['latestResourceData'] + `" style="display:none">
                      <p>Your browser does not support iframes.</p> -->
                  </iframe>
                </div>
              </div>
            </div>`


            }
            $("#searched_items").html(html_to_add)
            $("#item_details_card").html(item_details_card_html)
        }else{
            // console.log('map2')
            $("#retrieved_item_count").html("&nbsp;| &nbsp;Items retrieved : <span style='color:red'>"+data.length+"</span>");
            plot_points_on_map(data);
        }
        });
    }

        //For search bar 
    function search(tag) {
        
            $(".tag_item_count").html("");
            $.get("/search/catalogue/attribute?attribute-name=(tags)&attribute-value=((" + tag + "))", function(data) {
                // console.log(data)
                data = JSON.parse(data)
                if($(".__active").attr('id')=='list_view'){
                var html_to_add = "";
                var item_details_card_html = ""
                $("#retrieved_item_count").html("&nbsp;| &nbsp;Items retrieved : <span style='color:red'>"+data.length+"</span>");
                for (var i = data.length - 1; i >= 0; i--) {
                    html_to_add += 
      `            <div class="col-md-12 grid-margin stretch-card">
                  <div class="card">
                    <div class="card-body">
                      <div class="d-flex align-items-top mb-2">
                        <img src="https://image.flaticon.com/icons/svg/1481/1481951.svg" class="img-sm rounded-circle mr-3" alt="image">
                        <div class="mb-0 flex-grow">
                          <h5 class="mr-2 mb-0 text-info">` + data[i]['NAME'] + `</h5>
                          <p class="mb-0 font-weight-light">` + data[i]['itemDescription'] + `</p>
                        </div>
                         <button type="button" class="btn btn-outline-success btn-fw btn-detail" onclick="show_details_of(this,'` + i + `')">Details</button>
                        <!-- &nbsp;<button type="button" class="btn btn-outline-info btn-fw" onclick="show_latest_data('` + i + `','` + data[i]['latestResourceData'] + `')">Latest Data</button> -->
                      </div>
                      
                       <pre id="json-renderer-` + i + `" class="id_row"  style="height: 450px; display:none;">` + jsonPrettyHighlightToId(data[i]) + `</pre>
                      
                      <!--<iframe id="iframe-` + i + `" width="100%" height="100%" src="` + data[i]['latestResourceData'] + `" style="display:none">
                          <p>Your browser does not support iframes.</p> -->
                      </iframe>
                    </div>
                  </div>
                </div>`


                }
                $("#searched_items").html(html_to_add)
                $("#item_details_card").html(item_details_card_html)
                }else{
                // console.log("s",data.length)
                $("#retrieved_item_count").html("&nbsp;| &nbsp;Items retrieved : <span style='color:red'>"+data.length+"</span>");
                plot_points_on_map(data);
                }
            });
    }

    function show_all_items() {
        // console.log($(".nav-item").hasClass("_active"));
        if(!$(".nav-item").hasClass("_active")){
            $.toast({ 
              text : `Displaying all items. <br> Select tags for filtering`, 
              showHideTransition : 'fade',  // It can be plain, fade or slide
              bgColor : '#07cdae',              // Background color for toast
              textColor : '#000',            // text color
              allowToastClose : false,       // Show the close button or not
              hideAfter : 3500,              // `false` to make it sticky or time in miliseconds to hide after
              stack : 5,                     // `fakse` to show one stack at a time count showing the number of toasts that can be shown at once
              textAlign : 'center',            // Alignment of text i.e. left, right, center
              position : 'mid-center'       // bottom-left or bottom-right or bottom-center or top-left or top-right or top-center or mid-center or an object representing the left, right, top, bottom values to position the toast on page
            })
            if($(".__active").attr('id')=='list_view'){
         $(".tag_item_count").html("");
         
            // console.log(data)
            data = JSON.parse(localStorage.getItem("data"))
            $("#retrieved_item_count").html("&nbsp;| &nbsp;Items retrieved : <span style='color:red'>"+data.length+"</span>");
            var html_to_add = "";
            var item_details_card_html = ""
            for (var i = data.length - 1; i >= 0; i--) {
                html_to_add += 
  `            <div class="col-md-12 grid-margin stretch-card">
              <div class="card">
                <div class="card-body">
                  <div class="d-flex align-items-top mb-2">
                    <img src="https://image.flaticon.com/icons/svg/1481/1481951.svg" class="img-sm rounded-circle mr-3" alt="image">
                    <div class="mb-0 flex-grow">
                      <h5 class="mr-2 mb-0 text-info">` + data[i]['NAME'] + `</h5>
                      <p class="mb-0 font-weight-light">` + data[i]['itemDescription'] + ` &nbsp;&nbsp;&nbsp;<br><label class="badge"  style="background-color:`+getColorOf(getIndexOf(data[i]['accessInformation'][0]['accessVariables']['resourceClass']))+`">` + data[i]['accessInformation'][0]['accessVariables']['resourceClass'] + `</label></p>
                    </div>
                     <button type="button" class="btn btn-outline-success btn-fw btn-detail" onclick="show_details_of(this,'` + i + `')">Details</button>
                    <!-- &nbsp;<button type="button" class="btn btn-outline-info btn-fw" onclick="show_latest_data('` + i + `','` + data[i]['latestResourceData'] + `')">Latest Data</button> -->
                  </div>
                  
                   <pre id="json-renderer-` + i + `" class="id_row"  style="height: 450px; display:none;">` + jsonPrettyHighlightToId(data[i]) + `</pre>
                  
                  <!--<iframe id="iframe-` + i + `" width="100%" height="100%" src="` + data[i]['latestResourceData'] + `" style="display:none">
                      <p>Your browser does not support iframes.</p> -->
                  </iframe>
                </div>
              </div>
            </div>`


            }
            $("#searched_items").html(html_to_add)
            $("#item_details_card").html(item_details_card_html)
        
         }else{
            //Display all points in map plot_point_on_map([18.51957, 73.85535], "Hello Pop-up");
            plot_points_on_map(JSON.parse(localStorage.getItem("data")));
         }

        }else{

            $.get("/search/catalogue/attribute?attribute-name=(tags)&attribute-value=((" + ($("._active > .nav-link").html()).split(" &nbsp")[0] + ")", function(data) {
            // console.log(data)
            data = JSON.parse(data)
            if($(".__active").attr('id')=='list_view'){
                // console.log('list')
                var html_to_add = "";
                var item_details_card_html = ""

                for (var i = data.length - 1; i >= 0; i--) {
                html_to_add += 
                `            <div class="col-md-12 grid-margin stretch-card">
                <div class="card">
                <div class="card-body">
                  <div class="d-flex align-items-top mb-2">
                    <img src="https://image.flaticon.com/icons/svg/1481/1481951.svg" class="img-sm rounded-circle mr-3" alt="image">
                    <div class="mb-0 flex-grow">
                      <h5 class="mr-2 mb-0 text-info">` + data[i]['NAME'] + `</h5>
                      <p class="mb-0 font-weight-light">` + data[i]['itemDescription'] + ` &nbsp;&nbsp;&nbsp;<br><label class="badge"  style="background-color:`+getColorOf(getIndexOf(data[i]['accessInformation'][0]['accessVariables']['resourceClass']))+`">` + data[i]['accessInformation'][0]['accessVariables']['resourceClass'] + `</label></p>
                    </div>
                     <button type="button" class="btn btn-outline-success btn-fw btn-detail" onclick="show_details_of(this,'` + i + `')">Details</button>
                    <!-- &nbsp;<button type="button" class="btn btn-outline-info btn-fw" onclick="show_latest_data('` + i + `','` + data[i]['latestResourceData'] + `')">Latest Data</button> -->
                  </div>
                  
                   <pre id="json-renderer-` + i + `" class="id_row"  style="height: 450px; display:none;">` + jsonPrettyHighlightToId(data[i]) + `</pre>
                  
                  <!--<iframe id="iframe-` + i + `" width="100%" height="100%" src="` + data[i]['latestResourceData'] + `" style="display:none">
                      <p>Your browser does not support iframes.</p> -->
                  </iframe>
                </div>
                </div>
                </div>`


                }
                $("#searched_items").html(html_to_add)
                $("#item_details_card").html(item_details_card_html)
            }else{
                //Display points related to one tag
                // console.log('map')
                plot_points_on_map(data);
            }
        });
        }

    }    

    function get_location() { //user clicks button
        if ("geolocation" in navigator){ //check geolocation available 
            //try to get user current location using getCurrentPosition() method
            navigator.geolocation.getCurrentPosition(function(position){ 
                    $.get("https://nominatim.openstreetmap.org/reverse?format=json&lon="+position.coords.longitude+"&lat="+position.coords.latitude, function(data) {
                        $("#location").html("<i class='border-0 mdi mdi-map-marker'></i> " + data['display_name']);
                    });
                });
        }else{
            console.log("Browser doesn't support geolocation!");
        }
    }

    function objToString(obj) {
        var str = "";
        for (var p in obj) {
            // console.log(obj[p], typeof(obj[p]))
            if (typeof(obj[p]) != "object") {
                str = str + p + ": \t" + obj[p] + "<br>";
            }
        }
        return str;
    }

    var timer = 200;

    function show_details_of(e, id) {
        $(e).text($(e).text() == 'Details' ? 'Hide Details' : 'Details');
        $("#json-renderer-" + id).toggle(timer);
    }

    function show_latest_data(id, _url) {
        // $.ajax({
        //  url: _url,
        //  dataType: "jsonp",
        //  type: "GET",
        //  contentType: "application/json",
        //  crossDomain: true,
        //  success: function(data){
        //    console.log(data)
        //  },
        //  error: function(){
        //    console.log("error")
        //  }
        // });
        // $.get( _url, function( data ) {
        // $("latest_data_"+id).html(data);
        //    });
        $("iframe").hide(timer);
        $("#iframe-" + id).show(timer);

    }

    function refresh_tags() {
        $.get("list/catalogue/catalogue-item", function(data) {
            if (localStorage.getItem("data") == data) {
                $.toast({ 
                  text : `No updates found`, 
                  showHideTransition : 'fade',  // It can be plain, fade or slide
                  bgColor : '#07cdae',              // Background color for toast
                  textColor : '#000',            // text color
                  allowToastClose : false,       // Show the close button or not
                  hideAfter : 2500,              // `false` to make it sticky or time in miliseconds to hide after
                  stack : 5,                     // `fakse` to show one stack at a time count showing the number of toasts that can be shown at once
                  textAlign : 'center',            // Alignment of text i.e. left, right, center
                  position : 'mid-center'       // bottom-left or bottom-right or bottom-center or top-left or top-right or top-center or mid-center or an object representing the left, right, top, bottom values to position the toast on page
                })
                // $.toast("No new updates found")
            } else {
                performLocalStorage(data);
                populate_side_bar();
                $.toast({ 
                  text : "Tags Updated.", 
                  showHideTransition : 'slide',  // It can be plain, fade or slide
                  bgColor : '#07cdae',              // Background color for toast
                  textColor : '#eee',            // text color
                  allowToastClose : false,       // Show the close button or not
                  hideAfter : 2500,              // `false` to make it sticky or time in miliseconds to hide after
                  stack : 5,                     // `fakse` to show one stack at a time count showing the number of toasts that can be shown at once
                  textAlign : 'left',            // Alignment of text i.e. left, right, center
                  position : 'mid-center'       // bottom-left or bottom-right or bottom-center or top-left or top-right or top-center or mid-center or an object representing the left, right, top, bottom values to position the toast on page
                })
            }
        });
    }

    // console.log("l",localStorage.getItem("data"))

    function style(feature) {
        return {
            weight: 1.5,
            opacity: 1,
            fillOpacity: 1,
            radius: 6,
            fillColor: getColorOf(feature.properties.TypeOfIssue),
            color: "grey"

        };
    }


    var map;

    function changeView(view_name){
        if(view_name=="list_view"){
            $("#map_view").removeClass("__active");
            $("#list_view").addClass("__active");
            $("#searched_items").html("");
        }else{
            $("#list_view").removeClass("__active");
            $("#map_view").addClass("__active");
            $("#searched_items").html("<div id='mapid'></div>");
            $("#searched_items").removeClass("row");
            //get static map tiles, if possible https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png
            map = new L.Map('mapid');
            var osmUrl='https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';
            var osmAttrib='Map data © <a href="https://openstreetmap.org">OpenStreetMap</a> contributors';
            var osm = new L.TileLayer(osmUrl, {minZoom: 12, maxZoom: 100, attribution: osmAttrib,opacity:0.5});  
            map.setView(new L.LatLng(18.51957, 73.85535),12);
            map.addLayer(osm);
            L.marker([18.51957, 73.85535]).addTo(map).bindPopup('Pune');
            layerGroup = L.layerGroup().addTo(map);
            // plot_point_on_map([18.51957, 73.85535], "Pune");


            var legend = L.control({position: 'bottomright'});
            legend.onAdd = function (map) {

            var div = L.DomUtil.create('div', 'info legend');
            labels = ['<strong>Categories</strong>'],
            categories = localStorage.getItem("resourceClass").split(',')
            // console.log(categories)
            var colors = localStorage.getItem("resourceClassColors").split(',');
            for (var i = 0; i < categories.length; i++) {

                    div.innerHTML += 
                    labels.push(
                        '<span class="dots" style="background:' + colors[i] + '"></span> ' +
                    (categories[i] ? categories[i] : '+'));

                }
                div.innerHTML = labels.join('<br>');
            return div;
            };
            legend.addTo(map);
            // console.log("Added legends")
            }
        show_all_items();
    }












/******************************** Admin Dashboard functions ************************************/

function log(msg){
    var print = false;
    if(print){
        console.log(msg);
    }
}

function loader(e){
    e.html(`
        <div class="loader-demo-box">
                          <div class="square-box-loader">
                            <div class="square-box-loader-container">
                              <div class="square-box-loader-corner-top"></div>
                              <div class="square-box-loader-corner-bottom"></div>
                            </div>
                            <div class="square-box-loader-square"></div>
                          </div>
                        </div>
        `)
}

function read_item(e){
    if($(e).hasClass("active")){
        log("ALREADY ACTIVE")
        showSwal('auto-close-2')
        return;
    }
    $(".sidebar-nav-link").removeClass("active");
    $(e).addClass("active");
    $(".content-wrapper").html(`<div class="page-header">
              <h3 class="page-title"> Read Item </h3>
              <nav aria-label="breadcrumb">
                <ol class="breadcrumb">
                  <li class="breadcrumb-item"><a href="#">CRUD Operations</a></li>
                  <li class="breadcrumb-item active" aria-current="page">Read Item</li>
                </ol>
              </nav>
            </div>
            <div class="col-12 grid-margin">
                <div class="card">
                  <div class="card-body">
                    <h4 class="card-title">Catalogue items</h4>
                    <div class="row">
                      <div id="read_items" class="col-md-12 grid-margin">
                        
                      </div>
                    </div>
                  </div>
                </div>
              </div>`);
    loader($("#read_items"));
    $.get("https://10.156.14.149:8443/search/catalogue/attribute?", function(data) {
        $("#read_items").html(`<pre>`+jsonPrettyHighlightToId(JSON.parse(data))+`</pre>`)
    });
}



