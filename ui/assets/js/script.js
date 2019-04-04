    // Disable ctrl+s to prevent downloading source code 
    $(document).bind('keydown', function(e) {
        if (e.ctrlKey && (e.which == 83)) {
            e.preventDefault();
            // alert('Ctrl+S');
            return false;
        }
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

    function cache_cat() {
          return new Promise(function(resolve, reject) {
             $.get("/cat/search", function(data) {
                localStorage.setItem("data", data);
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
    //                 tag_cloud_html.push(`<a href="/cat/search/attribute?tags=(` + data[item_index]['tags'][tag_item_index] + `)" target="_blank"><button style="margin:10px;" type="button" class="btn btn-default">` + data[item_index]['tags'][tag_item_index] + `</button></a>`)
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
                if(data[item_index]['tags'][tag_item_index].toLowerCase()=="feeder" || data[item_index]['tags'][tag_item_index].toLowerCase()=="streetlight" || data[item_index]['tags'][tag_item_index].toLowerCase()=="streetlighting"){
                    continue;
                }
                if (!seen_tags_set.includes(data[item_index]['tags'][tag_item_index].toLowerCase())) {
                    seen_tags_set.push(data[item_index]['tags'][tag_item_index].toLowerCase())
                }
            }
        }

        for (var i = seen_tags_set.length - 1; i >= 0; i--) {

            //For sidebar
            tag_cloud_html.push(`<li class="nav-item" onclick="show_items_of(this,'` + seen_tags_set[i] + `','`+i+`')">
            <a class="nav-link" href="#">` + seen_tags_set[i] + ` &nbsp;
              <span id="tag_item_count-`+i+`" class="tag_item_count"></span>
            </a>
          </li>`)
        }

        $("#tags_side_bar").html(tag_cloud_html.sort());
        // $("#tags_side_bar").html(tag_cloud_html);
        tag_cloud_html = []

        //For search auto complete
        return seen_tags_set;
    }

    var options = {
        collapsed: true,
        rootCollapsable: true,
        withQuotes: true,
        withLinks: true
    };

    function show_items_of(e, tag, index) {
        $(".nav-item").removeClass("active");
        $(e).addClass("active");
         $(".tag_item_count").html("");
        $.get("/cat/search/attribute?tags=(" + tag + ")", function(data) {
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
        $.get("/cat/search", function(data) {
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
                localStorage.setItem("data", data);
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