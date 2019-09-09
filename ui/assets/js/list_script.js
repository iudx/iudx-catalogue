/*************************************************GLOBAL VARIABLES START*********************************************/
var tags_set=[]
var first_get_item_call_done=false
var filters="(id,resourceServerGroup,itemDescription,onboardedBy,accessInformation,resourceId,tags,secure)"
/*************************************************GLOBAL VARIABLES END***********************************************/







/*************************************************FUNCTION DECLARATIONS START*********************************************/

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
		alert("Error! Attribute-Name or Value missing");
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
            console.log(data)
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
					console.log(ui["item"]['label'])
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
				get_items_for_tag(ui["item"]['label'])
			}
		});
	}
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
		
		$("#_tbody_"+id).html(`
			<tr>
			      <th scope="row">resource-Id</th>
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
				<!--<a href="`+ get_latest_data_url(_id,data[0]["resourceServerGroup"]["value"],data[0]["resourceId"]["value"]) +`" target="_blank">Latest Data</a>   |  -->
				<a href="`+data[0]["refBaseSchema"]["value"]+`" target="_blank">Base Schema </a> |
				<a href="`+data[0]["refDataModel"]["value"]+`" target="_blank">Data Model </a>
			</p>
			`);
			// <a href="`+data[0]["latestResourceData"]["object"]+`" target="_blank">Latest Data </a>
		$("#details_section_"+id).toggle();
	});
}

function get_latest_data_url(id, rsg, rid){
	if(id=="rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/safetipin/safetipin/safetyIndex"){
		return 'https://pune.iudx.org.in/api/1.0.0/resource/search/safetypin/18.56581555/73.77567708/10'
	}else{
		return `https://pune.iudx.org.in/api/1.0.0/resource/latest/`+rsg+`/`+rid
	}
}

function resource_id_to_html_id(resource_id){
	var replace_with = "_";
	return resource_id.replace(/\/|\.|\s|\(|\)|\<|\>|\{|\}|\,|\"|\'|\`|\*|\;|\+|\!|\#|\%|\^|\&|\=|\₹|\$|\@/g,replace_with)
}

function _get_latest_data(_resource_id, _token){
	$.ajax({
	  url: "https://pune.iudx.org.in/api/1.0.0/resource/search/safetypin/18.56581555/73.77567708/10",
	  type: 'get',
      headers: {"token": _token},
	  success: function (data) {
	  	
        // alert("Success! \n"+data)

        var w = window.open('about:blank');
    	w.document.open();
    	w.document.write("<pre>"+data+"</pre><script src='https://cdnjs.cloudflare.com/ajax/libs/jsoneditor/6.4.1/jsoneditor.js'></script>");
      }
	});
}

function _get_security_based_latest_data_link(_resource_id, _resourceServerGroup, _rid, token){
	if(_resource_id=="rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/safetipin/safetipin/safetyIndex"){
		return `<button class="btn btn-secondary" onclick="_get_latest_data('`+_resource_id+`','`+token+`')">Get Full Latest Data</button>`
	}else{
		return `<a href="`+ get_latest_data_url(_resource_id,_resourceServerGroup,_rid) +`" target="_blank">Get Latest Data</a>`
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
	  	// console.log(data.access_token)
	  	
        // $('#token_section_'+resource_id_to_html_id(resource_id)).html($('#token_section_'+resource_id_to_html_id(resource_id)).html());
        $('#token_section_'+resource_id_to_html_id(resource_id)).html(
        																`<b>Token</b>: <span id="token_value_`+resource_id_to_html_id(resource_id)+`">` + data.access_token + `</span>`
        																+ `<span><img class="secure_icon" src="../assets/img/icons/copy.svg" onclick="copyToClipboard('`+resource_id_to_html_id(resource_id)+`')"></span> | `
        																+ _get_security_based_latest_data_link(resource_id,resourceServerGroup, rid, data.access_token))
        alert("Success! \nToken received: " + data.access_token)
        $('#token_section_'+resource_id_to_html_id(resource_id)).toggle();
         	
      },
      error: function (jqXHR, exception) {
      	alert("Unauthorized access! Please get a token.")
      }
	});
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
	alert("Token copied!")
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
		  <span class="float-left"><img src='`+
		  ((is_public) ? "../assets/img/icons/public_shield.svg" : "../assets/img/icons/secure_item.svg")
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
		    <button class="btn btn-secondary"><a href="`+ get_latest_data_url(json_obj["id"],json_obj["resourceServerGroup"]["value"],json_obj["resourceId"]["value"]) +`" target="_blank" style="color:white">Get Latest Data</a></button>
		    
		    </div>
		  </div>
		  <div id="token_section_`+resource_id_to_html_id(json_obj.id)+`" class="token_section"></div>
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

