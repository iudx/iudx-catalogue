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
        $.get("/cat/search", function(data) {
            localStorage.setItem("data", data);
        });
    }

    if (localStorage.getItem("data") == null) {
        cache_cat();
    }

    function display_tag_cloud() {
        var seen_tags_set = [];
        var tag_cloud_html = [];
        data = JSON.parse(localStorage.getItem("data"))
        for (var item_index = 0; item_index < data.length - 1; item_index++) {
            for (var tag_item_index = 0; tag_item_index < data[item_index]['tags'].length - 1; tag_item_index++) {
                if (!seen_tags_set.includes(data[item_index]['tags'][tag_item_index])) {
                    seen_tags_set.push(data[item_index]['tags'][tag_item_index])
                    tag_cloud_html.push(`<a href="/cat/search/attribute?tags=(` + data[item_index]['tags'][tag_item_index] + `)" target="_blank"><button style="margin:10px;" type="button" class="btn btn-default">` + data[item_index]['tags'][tag_item_index] + `</button></a>`)
                }
            }
        }
        seen_tags_set = []
        $("#tag_cloud").html(tag_cloud_html);
        tag_cloud_html = []
    }

    // display_tag_cloud();

    var seen_tags_dict = {}

    function populate_side_bar() {
        var seen_tags_set = [];
        var tag_cloud_html = [];
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
            //For search auto complete
            seen_tags_dict[seen_tags_set[i]] = null;

            //For sidebar
            tag_cloud_html.push(`<li class="nav-item" onclick="show_items_of(this,'` + seen_tags_set[i] + `','`+i+`')">
            <a class="nav-link center" href="#">
              <p>` + seen_tags_set[i] + ` <span id="tag_item_count-`+i+`" class="tag_item_count"></span></p>
            </a>
          </li>`)
        }

        seen_tags_set = []
        // $("#tags_side_bar").html(tag_cloud_html.sort());
        $("#tags_side_bar").html(tag_cloud_html);
        tag_cloud_html = []
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
        $.get("/cat/search/attribute?tags=(" + tag + ")", function(data) {
            // console.log(data)
            data = JSON.parse(data)
            var html_to_add = "";
            var item_details_card_html = ""
            $(".tag_item_count").html("");
            $("#tag_item_count-"+index).html("("+data.length+")");
            for (var i = data.length - 1; i >= 0; i--) {
                html_to_add += `
             <div class="card">
             
    <div class="card-content" onclick="show_details_of(this,'` + i + `')">
      <span class="card-title activator blue-text text-darken-4">` + data[i]['NAME'] + `</span>
      <!--span>Status:` + data[i]['__itemStatus'] + `</span><br-->
      <span>` + data[i]['itemDescription'] + `</span>
    </div>
  </div>`
    item_details_card_html += `<div id="` + i + `" class="row id_row" style="display:none;">
    <div class="col s12">
      <div class="card white">
        <div class="card-content black-text">
          <span class="card-title">` + data[i]['NAME'] + `</span><hr>
          <pre id="json-renderer-` + i + `"  style="height: 450px;">` + jsonPrettyHighlightToId(data[i]) + `</pre>
        </div>
        <div class="card-action">
          <div id="latest_data_` + i + `"></div>
          <a class="blue-text lighten-1" onclick="show_latest_data('` + i + `','` + data[i]['latestResourceData'] + `')">Latest Data</a>
          <iframe id="iframe-` + i + `" width="100%" height="100%" src="` + data[i]['latestResourceData'] + `" style="display:none">
              <p>Your browser does not support iframes.</p>
          </iframe>
        </div>                      
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
                        $("#location").html("<i class='material-icons'>location_on</i> " + data['display_name']);
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

    function show_details_of(e, id) {
        $(".card-content").css('background-color', 'white');
        $(".card-content").css('color', 'black');
        $(e).css('background-color', '#b2ebf2');
        $(e).css('color', 'black');
        $(".id_row").hide();
        $("#" + id).show();
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
        $("iframe").hide();
        $("#iframe-" + id).show();

    }

    function refresh_tags() {
        $.get("/cat/search", function(data) {
            if (localStorage.getItem("data") == data) {
                M.toast({
                    html: 'No new updates found',
                    classes: 'rounded',
                    displayLength: 1000
                })
            } else {
                localStorage.setItem("data", data);
                populate_side_bar();
                M.toast({
                    html: 'Tags Updated.',
                    classes: 'rounded',
                    displayLength: 1000
                })
            }
        });
    }

    // console.log("l",localStorage.getItem("data"))