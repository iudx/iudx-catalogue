/*************************************************GLOBAL VARIABLES START*********************************************/
var tags_set=[]
var rsg_set=[]
var provider_set=[]
var first_get_item_call_done=false
var filters="(id,resourceServerGroup,itemDescription,onboardedBy,accessInformation,resourceId,tags,secure)"
var page_limit = 10;
var max_visible_pagesinpagination_bar = 10;
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
	// //console.log("dislpaying item from:"+from+" to:"+to + " " + (global_data.length/get_page_limit() + ((global_data.length%get_page_limit())>0) ? 1 : 0));
}

function populate_pagination_section(){
    // init bootpag
    var data_length = get_global_data().length
    $('#page-selection').bootpag({
        total: (data_length/get_page_limit() + (((data_length%get_page_limit())>0) ? 1 : 0)),
        maxVisible: max_visible_pagesinpagination_bar,
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

    display_paginated_search_results(1);

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

function get_item_count(__data){
	var _c=0;
	for (var i = __data.length - 1; i >= 0; i--) {
		if(__data[i]['itemType']['value']=="resourceItem"){
			_c+=1;
		}
	}
	return _c;
}

/*
function _get_item_count(__url){
    return new Promise((resolve, reject) => {
        $.ajax({
            "url": __url,
            "async": true,
            "crossDomain": true,
            "processData": false,
            "method": 'GET',
            // dataType: 'json',
            success: function (data) {
                resolve(get_item_count(JSON.parse(data)))
            },
            error: function (error) {
                reject(error)
            },
            timeout: 30000 // sets timeout to 30 seconds
        })
    })
}
*/

function get_items(_attr_name,_attr_value){
	if(is_attr_empty(_attr_name,_attr_value)){
		return;
	}

	if(!first_get_item_call_done){
		first_get_item_call_done=true;
		display_search_section();
	}

	var _temp_a_v = _attr_value

	if(_attr_name=="resourceServerGroup"){
		_attr_value=cat_conf['resource_server_group_head']+_attr_value
	}else if(_attr_name=="provider"){
		_attr_value=cat_conf['provider_head']+_attr_value
	}
	
	$(".se-pre-con").fadeIn("slow");
	
	$.get("/catalogue/v1/search?attribute-name=("+_attr_name+")&attribute-value=("+_attr_value+")", function(data) {
            // $("#searched_items").text(data);
		data=JSON.parse(data)
		set_data_globally(data);
		$("#retrieved_items_count").html("About " + get_item_count(data) + " results for " + _temp_a_v + " (Attribute: " + _attr_name + ") | <a href='/map'>Go to Map View</a>");
		$("#searched_items").html("");
		for (var i = 0; i < data.length; i++) {
			$("#searched_items").append(json_to_htmlcard(data[i]));
		}
		populate_pagination_section();
			
		$(".se-pre-con").fadeOut("slow");
        });

	// $( "#_value" ).autocomplete({
	//       source: seen_tags_set
	// });

	// $( "#value" ).autocomplete({
	//       source: seen_tags_set
	// });
}


function getFooterContent(){
	return `<p>&copy; 2019 <a href="https://iudx.org.in" target="_blank">IUDX </a></p>`
	// return `<p>&copy; 2019 <a href="https://iudx.org.in" target="_blank">IUDX </a> | Read the  <a href="`+ cat_conf['api_docs_link'] +`" target="_blank">Doc</a> <br> ` + get_icon_attribution_html("list_icon_attr") + `</p>`
}

function set_attr_value(__attr_name,__attr_value) {
	// ////console.log("v:",$( "#value" ).is(':visible'))
	// ////console.log("_v:",$( "#_value" ).is(':visible'))
	if($( "#value" ).is(':visible')){
			$( "#value" ).autocomplete({
				source: __attr_value,
				select: function( event, ui ) {
					get_items(__attr_name, ui["item"]['label'])
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
			source: __attr_value,
			select: function( event, ui ) {
				get_items(__attr_name, ui["item"]['label'])
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
	var id = resource_id_to_html_id(_id)
	// console.log($("#details_section_"+id).is(':visible'))
	if(!($("#details_section_"+id).is(':visible'))) {
    	$.get("/catalogue/v1/items/" + _id , function(data) {
			data=JSON.parse(data)
			// //console.log(data)
			// //console.log(data[0]["resourceId"]["value"])
			// //console.log(data[0]["itemDescription"])
			// //console.log(data[0]["itemType"]["value"])
			// //console.log(data[0]["provider"]["value"])
			// //console.log(data[0]["createdAt"]["value"])
			// //console.log(data[0]["resourceServerGroup"]["value"])
			// //console.log(data[0]["itemStatus"]["value"])
			// //console.log(data[0]["refBaseSchema"]["value"])
			// //console.log(data[0]["refDataModel"]["value"])
			////console.log(id);
			
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

				$("#extra_links_"+id).html(`
				<p>
					<!--<a href="`+ get_latest_data_url() +`">Latest Data</a>   |  -->
					<a href="`+data[0]["refBaseSchema"]["value"]+`" target="_blank">Base Schema </a> |
					<a href="`+data[0]["refDataModel"]["value"]+`" target="_blank">Data Model </a>
				</p>
				`);
		});
	}
	$("#details_section_"+id).toggle();
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
	if(json_obj['id'].split("/").length < 5){
		return ``
	}
	else{
		var openapi_url = "blah_blah"
		// var openapi_url = json_obj["accessInformation"]["value"][0]["accessObject"]["value"]
		// var openapi_url = json_obj["accessInformation"]["value"][0]["accessObject"]["value"]
		// //console.log(openapi_url)
		var is_public = (json_obj['secure']||[]).length === 0;
		var rat_btn_html=`<button class="btn btn-success" onclick="request_access_token('` + json_obj.id + `', '`+ json_obj["resourceServerGroup"]["value"] + `', '`+ json_obj["resourceId"]["value"] + `')" style="background-color:green">Request Access Token</button>`
		var s = json_obj["id"].split("/")
		return `
			<div class="col-12 card-margin-top">
			<div class="card">
			  <h5 class="card-header card-header-color">
			  <span class="float-left" style="padding-right:7.5px;"><img src='`+
			  ((is_public) ? "../assets/img/icons/green_unlock.svg" : "../assets/img/icons/red_lock.svg")
			  +`' class='img-fluid secure_icon'></span>` + get_horizontal_spaces(3) + s.splice(2).join("/") + " <b>BY</b> " + s[0]  + `</h5>
			  <div class="card-body">
			    <h5 class="card-title">` + json_obj["itemDescription"] + `</h5>
			    <strong>Item-ID</strong>: `+json_obj['id']+`<br>
			    <strong>Onboarded-By</strong>: `+json_obj['onboardedBy']+`<br>
			    <strong>Access</strong>: `+ (is_public ? "Public": "Requires Authentication") +`<br>
			    <div id="btn_`+resource_id_to_html_id(json_obj.id)+`">
			    <button class="btn btn-primary" onclick="show_details('`+ json_obj.id +`')">Details</button>
			    <!--button class="btn btn-success" onclick="display_swagger_ui('` + openapi_url + `')">API Details</button-->
			    `+ ((is_public)?"":rat_btn_html) +`
			    <a href="#" style="color:white"  class="data-modal" onclick="display_latest_data(event, this, '`+json_obj['id']+`')"><button class="btn btn-danger">Get Latest Data</button></a>
			    <a href="#" style="color:white"  class="data-modal" onclick="display_temporal_data(event, this, '`+json_obj['id']+`')"><button class="btn btn-warning">Get Temporal Data</button></a>
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
				<p id="extra_links_`+resource_id_to_html_id(json_obj.id)+`"></p>
			  </div>
			</div>
			</div>
		`	
	}
}

/*************************************************FUNCTION DECLARATIONS START*********************************************/








/*************************************************EVENT BINDINGS START*********************************************/


// Set up Footer, filter seen_tags_set
$(document).ready(function(){
	
	$("body").fadeIn(1000);
	$("#landing_section").fadeIn();
	
	$.get("/catalogue/v1/search", function(data) {
		$("#resource_item_count").html(get_item_count(JSON.parse(data)));
	});
	
	$.get("/catalogue/internal_apis/list/tags", function(data) {
		tags_set=JSON.parse(data)
	});
	
	$.get("/catalogue/internal_apis/list/resourceServerGroup", function(data) {
		rsg_set=JSON.parse(data)
		for (var i = rsg_set.length - 1; i >= 0; i--) {
			rsg_set[i]=rsg_set[i].split(cat_conf['resource_server_group_head'])[1]
		}
	});
	
	$.get("/catalogue/internal_apis/list/provider", function(data) {
		provider_set=JSON.parse(data)
		$("#provider_count").html(provider_set.length);
		for (var i = provider_set.length - 1; i >= 0; i--) {
			provider_set[i]=provider_set[i].split(cat_conf['provider_head'])[1]
		}
	});

	$("#landing_footer, #normal_footer").html(getFooterContent());

});





// Capture select on change effect for populating autosuggest with attribute values 
$('select').on('change', function() {
	var _arr = []
	if(this.value == "tags"){
		_arr = tags_set
	}else if(this.value == "resourceServerGroup"){
		_arr = rsg_set
	}else if(this.value == "provider"){
		_arr = provider_set
	}
	set_attr_value(this.value, _arr)
});




// Capture search input click
$(".ui-menu").on('click',function(){
	////console.log("s",this.value)
});


/*************************************************EVENT BINDINGS END*********************************************/









/********************************************************************************************/



/********************************************************************************************/
