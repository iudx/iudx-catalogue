/*************************************************GLOBAL VARIABLES START*********************************************/
var tags_set=[]
var first_get_item_call_done=false
var filters="(id,resourceServerGroup,itemDescription,onboardedBy,accessInformation,resourceId,tags,secure)"
var page_limit = 10;
var __DATA;
/*************************************************GLOBAL VARIABLES END***********************************************/


/*************************************************FUNCTION DECLARATIONS START*********************************************/

function get_global_data(){
	return __DATA;
}

function get_page_limit(){
	return page_limit;
}

// To be used when UI has the ability to showcase this feature
function set_page_limit(_page_limit){
	page_limit = _page_limit;
}

function set_data_globally(_data){
	__DATA = _data;
}

function min(val1, val2){
	return Math.min(val1, val2);
}

function display_paginated_search_results(page_num){
	var global_data = get_global_data();
	$("#searched_items").html("");
	var from = min(((page_num-1)*get_page_limit()),global_data.length);
	var to = min(((page_num)*get_page_limit()-1), global_data.length);
	for (var i=from;i < to; i++) {
		$("#searched_items").append(json_to_htmlcard(global_data[i]));	
	}
	// console.log("dislpaying item from:"+from+" to:"+to + " " + (global_data.length/get_page_limit() + ((global_data.length%get_page_limit())>0) ? 1 : 0));
}

function populate_pagination_section(){
    // init bootpag
    var data_length = get_global_data().length
    $('#page-selection').bootpag({
        total: (data_length/get_page_limit() + (((data_length%get_page_limit())>0) ? 1 : 0)),
        maxVisible: 5,
        leaps: true,
		next: '>',
		prev: '<',
	    firstLastUse: true,
	    first: '<<',
	    last: '>>',
	    wrapClass: 'pagination',
	    activeClass: 'page-active',
	    disabledClass: 'disabled',
	    nextClass: 'next',
	    prevClass: 'prev',
	    lastClass: 'last',
	    firstClass: 'first'
    }).on("page", function(event, /* page number here */ num){
          display_paginated_search_results(num);
    });
}

function display_search_section(_attr_name,_attr_value){
	if(is_attr_empty(_attr_name,_attr_value)){
		return;
	}
	display_search_section();
	get_items(_attr_name,_attr_value);
}

function display_saved_search_section(){
	$(".section").fadeOut(200);
	$("body").css("background-image","none");
	$("#search_section").fadeIn(1500);
	// get_items();
}


function display_swagger_ui(_openapi_url){
	$(".section").fadeOut(200);
	$("body").css("background-image","none");
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

function is_attr_empty(_attr_name,_attr_value){
	if(_attr_name === "" || _attr_value === ""){
		_alertify("Error!!!", "Attribute-Name or Value missing");
		return true;
	}
}

function display_search_section(){
	$(".section").fadeOut(200);
	$("body").css("background-image","none");
	$("#search_section").fadeIn(1500);
}

function get_items(_attr_name,_attr_value){
	// console.log(_attr_name,_attr_value)
	if(is_attr_empty(_attr_name,_attr_value)){
		return;
	}

	if(!first_get_item_call_done){
		first_get_item_call_done=true;
		display_search_section();
	}

	console.log(first_get_item_call_done)

	let seen_tags_set = [];
	$.get("/catalogue/v1/search?attribute-name=("+_attr_name+")&attribute-value=("+_attr_value+")", function(data) {
            // $("#searched_items").text(data);
			data=JSON.parse(data)
			set_data_globally(data);
			$("#searched_items").html("");
            for (var i = 0; i < data.length; i++) {
                $("#searched_items").append(json_to_htmlcard(data[i]));
                for (var tag_i = 0; tag_i < data[i]['tags']['value'].length - 1; tag_i++) {
                // if(data[i]['tags'][tag_i].toLowerCase()=="feeder" || data[i]['tags'][tag_i].toLowerCase()=="streetlight" || data[i]['tags'][tag_i].toLowerCase()=="streetlighting"){
                //     continue;
                // }
                if (!seen_tags_set.includes(data[i]['tags']['value'][tag_i].toLowerCase())) {
                    seen_tags_set.push(data[i]['tags']['value'][tag_i].toLowerCase())
                }
            }
            }
            populate_pagination_section();
        });

	// $( "#_value" ).autocomplete({
	//       source: seen_tags_set
	// });

	// $( "#value" ).autocomplete({
	//       source: seen_tags_set
	// });
}


function get_items_for_tag(tag){
	let seen_tags_set = [];

	$.get("/catalogue/v1/search?attribute-name=(tags)&attribute-value=((" + tag + "))&attribute-filter="+filters, function(data) {
            // $("#searched_items").text(data);
            $("#searched_items").html("");

            data=JSON.parse(data)
            // __DATA=data;
            set_data_globally(data);
            // console.log(data)
            if(!$('#searched_items').is(':visible')) {
				    display_search_section();
				}
            $("#retrieved_items_count").html("About " + data.length + " results for " + tag);
            for (var i = 0; i < data.length; i++) {
                //console.log(data[i]);
                $("#searched_items").append(json_to_htmlcard(data[i]));
                for (var tag_i = 0; tag_i < data[i]['tags'].length - 1; tag_i++) {
                // if(data[i]['tags'][tag_i].toLowerCase()=="feeder" || data[i]['tags'][tag_i].toLowerCase()=="streetlight" || data[i]['tags'][tag_i].toLowerCase()=="streetlighting"){
                //     continue;
                // }
                if (!seen_tags_set.includes(data[i]['tags'][tag_i].toLowerCase())) {
                    seen_tags_set.push(data[i]['tags'][tag_i].toLowerCase())
                }
            }
            }
            populate_pagination_section();
        });

	// $( "#_value" ).autocomplete({
	//       source: seen_tags_set
	// });

	// $( "#value" ).autocomplete({
	//       source: seen_tags_set
	// });
}


function getFooterContent(){
	return `<p>&copy; 2019 <a href="https://iudx.org.in">IUDX </a> | Read the  <a href="https://docs.google.com/document/d/12kQteMgxINPjZUVaNBqvtEYJEfqDn7r7QWbL74o7wPQ/edit?usp=sharing">Doc</a> <br> <span style="font-size: 15px;">Icon made by <a href="https://www.flaticon.com/authors/freepik">Freepik</a> from <a href="https://www.flaticon.com">flaticon.com</a>.</span></p>`
}

function set_tags(_tags_set) {
	// //console.log("v:",$( "#value" ).is(':visible'))
	// //console.log("_v:",$( "#_value" ).is(':visible'))
	if($( "#value" ).is(':visible')){
			$( "#value" ).autocomplete({
				source: _tags_set,
				select: function( event, ui ) {
					// console.log(ui["item"]['label'])
					get_items_for_tag(ui["item"]['label'])
				}
				// ,
				// select: function (e, ui) {
				// 	alert("selected!", e);
				// },
				// change: function (e, ui) {
				// 	alert("changed!", e, ui);
				// }
			});
 		}

	if($( "#_value" ).is(':visible')){
		$( "#_value" ).autocomplete({
			source: _tags_set,
			select: function( event, ui ) {
				console.log(ui);
				get_items_for_tag(ui["item"]['label'])
			}
		});
	}
}



function get_horizontal_spaces(space_count){
	var horizontal_space_str=""
	for (var i = space_count.length - 1; i >= 0; i--) {
		horizontal_space_str+="&nbsp;"
	}
	return horizontal_space_str;
}

function copyToClipboard(element_id) {
	var $temp = $("<input>");
	$("body").append($temp);
	$temp.val($("#token_value_"+element_id).text()).select();
	// $("#copied_"+element_id).html("Token copied!")
	document.execCommand("copy");
	$temp.remove();
	_alertify("Success!!!", "Token copied!")
	// $.sweetModal({
	//   title: 'Token copied!',
	//   theme: $.sweetModal.THEME_DARK
	// });
}

function display_json_response_in_modal(json_obj){
		$.sweetAlert({
		  content: jsonPrettyHighlightToId(json_obj),
		  // $.sweetModal.ICON_SUCCESS
		  // $.sweetModal.ICON_WARNING
		  // $.sweetModal.ICON_ERROR
		  icon: $.sweetModal.ICON_SUCCESS

		});
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
		$("#details_section_"+id).html("")
		$("#_tbody_"+id).html(`
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
		`);
		 // <tr>
			//       <th scope="row">Authorization Server Info</th>
			//       <td>`+ data[0]["authorizationServerInfo"]["value"]["authServer"] +` | Type: `+ data[0]["authorizationServerInfo"]["value"]["authType"] +`</td>
		 //    </tr>

			$("#details_section_"+id).append(`
			<p>
				<!--<a href="`+ get_latest_data_url(_id,data[0]["resourceServerGroup"]["value"],data[0]["resourceId"]["value"]) +`">Latest Data</a>   |  -->
				<a href="`+data[0]["refBaseSchema"]["value"]+`" target="_blank">Base Schema </a> |
				<a href="`+data[0]["refDataModel"]["value"]+`" target="_blank">Data Model </a>
			</p>
			`);
			// <a href="`+data[0]["latestResourceData"]["object"]+`" target="_blank">Latest Data </a>
		$("#details_section_"+id).toggle();
	});
}

function display_swagger_ui(_openapi_url){
    $(".section").fadeOut(200);
    $("body").css("background-image","none");
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

function json_to_htmlcard(json_obj){
	var openapi_url = json_obj["accessInformation"]["value"][0]["accessObject"]["value"]
	// var openapi_url = json_obj["accessInformation"]["value"][0]["accessObject"]["value"]
	// console.log(openapi_url)
	var is_public = (json_obj['secure']||[]).length === 0;
	var rat_btn_html=`<button class="btn btn-success" onclick="request_access_token('` + json_obj.id + `', '`+ json_obj["resourceServerGroup"]["value"] + `', '`+ json_obj["resourceId"]["value"] + `')" style="background-color:green">Request Access Token</button>`
	var s = json_obj["id"].split("/")
	return `
		<div class="col-12 card-margin-top">
		<div class="card">
		  <h5 class="card-header card-header-color">
		  <span class="float-left" style="padding-right:7.5px;"><img src='`+
		  ((is_public) ? "../assets/img/icons/green_unlock.svg" : "../assets/img/icons/red_lock.svg")
		  +`' class='img-fluid secure_icon'></span>` + get_horizontal_spaces(3) + s.splice(2).join("/") + " by " + s[0]  + `</h5>
		  <div class="card-body">
		    <h5 class="card-title">` + json_obj["itemDescription"] + `</h5>
		    <strong>Item-ID</strong>: `+json_obj['id']+`<br>
		    <strong>Onboarded-By</strong>: `+json_obj['onboardedBy']+`<br>
		    <strong>Access</strong>: `+ (is_public ? "Public": "Requires Authentication") +`<br>
		    <div id="btn_`+resource_id_to_html_id(json_obj.id)+`">
		    <button class="btn btn-primary" onclick="show_details('`+ json_obj.id +`')">Details</button>
		    <button class="btn btn-info" onclick="display_swagger_ui('` + openapi_url + `')">APIs Details</button>
		    `+ ((is_public)?"":rat_btn_html) +`
		    <a href="`+ get_latest_data_url(json_obj["id"],json_obj["resourceServerGroup"]["value"],json_obj["resourceId"]["value"]) +`" style="color:white"  class="data-modal" onclick="display_latest_data(event, this)"><button class="btn btn-secondary">Get Latest Data</button></a>
		    
		    </div>
		     <div id="token_section_`+resource_id_to_html_id(json_obj.id)+`" class="token_section"></div>
		  </div>
		  <div id="details_section_`+resource_id_to_html_id(json_obj.id)+`" class="details_section">
		  	<table class="table table-borderless table-dark">
			  <thead>
			  	<tr></tr>
			  </thead>
			  <tbody id="_tbody_`+resource_id_to_html_id(json_obj.id)+`">

			  </tbody>
			</table>
		  </div>
		</div>
		</div>
	`	
}

/*************************************************FUNCTION DECLARATIONS START*********************************************/












/*************************************************EVENT BINDINGS START*********************************************/


// Set up Footer, filter seen_tags_set
$(document).ready(function(){
	$("body").fadeIn(1000);
	$("#landing_section").fadeIn();
	let seen_tags_set = [];
	$.get("/catalogue/v1/search", function(data) {
			// $("#searched_items").text(data);
			//console.log("RRRRRRRR1");
			data=JSON.parse(data)
			//console.log("RRRRRRRR");
            for (var i = 0; i < data.length; i++) {                
                for (var tag_i = 0; tag_i < data[i]['tags']['value'].length - 1; tag_i++) {
                // if(data[i]['tags'][tag_i].toLowerCase()=="feeder" || data[i]['tags'][tag_i].toLowerCase()=="streetlight" || data[i]['tags'][tag_i].toLowerCase()=="streetlighting"){
                //     continue;
                // }
                if (!seen_tags_set.includes(data[i]['tags']['value'][tag_i].toLowerCase())) {
                    seen_tags_set.push(data[i]['tags']['value'][tag_i].toLowerCase())
                }
            }
            }
        });
	tags_set=seen_tags_set;

	$("#landing_footer, #normal_footer").html(getFooterContent()	);
	$.get("/catalogue/v1/count", function(data) {
		$("#resource_item_count").html(JSON.parse(data)["Count"]);
	});

});





// Capture select on change effect for populating tags autosuggest 
$('select').on('change', function() {
	if(this.value == "tags"){
		set_tags(tags_set)
}else{
	set_tags=[]
}
//console.log( this.value );
});




// Capture search input click
$(".ui-menu").on('click',function(){
	//console.log("s",this.value)
});


/*************************************************EVENT BINDINGS END*********************************************/









/********************************************************************************************/



/********************************************************************************************/

