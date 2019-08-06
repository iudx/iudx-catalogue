/*************************************************GLOBAL VARIABLES START*********************************************/
var tags_set=[]
/*************************************************GLOBAL VARIABLES END***********************************************/







/*************************************************FUNCTION DECLARATIONS START*********************************************/

function display_search_section(){
	$(".section").fadeOut(200);
	$("body").css("background-image","none");
	$("#search_section").fadeIn(1500);
	get_items();
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


function get_items(){
	let seen_tags_set = [];
	$.get("/list/catalogue/resource-item", function(data) {
            // $("#searched_items").text(data);
            data=JSON.parse(data)
            for (var i = 0; i < data.length; i++) {
                // $("#searched_items").append(json_to_htmlcard(data[i]));
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


function get_items_for_tag(tag){
	let seen_tags_set = [];

	$.get("/search/catalogue/attribute?attribute-name=(tags)&attribute-value=((" + tag + "))", function(data) {
            // $("#searched_items").text(data);
            $("#searched_items").html("");
            data=JSON.parse(data)
            $("#retrieved_items_count").html("About " + data.length + " results for " + tag);
            for (var i = 0; i < data.length; i++) {
                // $("#searched_items").append(json_to_htmlcard(data[i]));
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
	return `<p>&copy; 2019 <a href="https://iudx.org.in">IUDX </a> | Read the  <a href="https://docs.google.com/document/d/12kQteMgxINPjZUVaNBqvtEYJEfqDn7r7QWbL74o7wPQ/edit?usp=sharing">Doc</a>.</p>`
}

function set_tags(_tags_set) {
	// console.log("v:",$( "#value" ).is(':visible'))
	// console.log("_v:",$( "#_value" ).is(':visible'))
	if($( "#value" ).is(':visible')){
			$( "#value" ).autocomplete({
				source: _tags_set,
				select: function( event, ui ) {
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

function show_details(id){
	$.get("/search/catalogue/attribute?attribute-name=(id)&attribute-value=((" + id + "))", function(data) {
		data=JSON.parse(data)
		console.log(data, data["NAME"])

		$("#_tbody_"+id).html(`
			<tr>
			      <th scope="row">Name</th>
			      <td>`+ data[0]["NAME"] +`</td>
		    </tr>
		    <tr>
			      <th scope="row">Description</th>
			      <td>`+ data[0]["itemDescription"] +`</td>
		    </tr>
		    <tr>
			      <th scope="row">Type</th>
			      <td>`+ data[0]["item-type"] +`</td>
		    </tr>
		    <tr>
			      <th scope="row">Provider</th>
			      <td>`+ data[0]["provider"]["entityId"] +`</td>
		    </tr>
		    <tr>
			      <th scope="row">Created-On</th>
			      <td>`+ data[0]["__createdAt"] +`</td>
		    </tr>
		    <tr>
			      <th scope="row">Status</th>
			      <td>`+ data[0]["Status"] +`</td>
		    </tr>
		`);
			$("#details_section_"+id).append(`
			<p>
				<a href="`+data[0]["latestResourceData"]+`" target="_blank">Latest Data </a> |  
				<a href="`+data[0]["refBaseSchema"]+`" target="_blank">Base Schema </a> |
				<a href="`+data[0]["refDataModel"]+`" target="_blank">Data Model </a> 
			</p>
			`);
		$("#details_section_"+id).toggle();
	});
}



function json_to_htmlcard(json_obj){
	return `
		<div class="col-12 card-margin-top">
		<div class="card">
		  <h5 class="card-header card-header-color">` + json_obj["NAME"] + `</h5>
		  <div class="card-body">
		    <h5 class="card-title">` + json_obj["itemDescription"] + `</h5>
		    <button class="btn btn-primary" onclick="show_details('`+ json_obj["id"] +`')">Details</button>
		    <button class="btn btn-info" onclick="display_swagger_ui('` + json_obj["accessInformation"][0]["accessSchema"] + `')">APIs Details</button>
		    <!--button class="btn btn-secondary">Request Access Token (For Non-Public data)???</button-->
		  </div>
		  <div id="details_section_`+json_obj["id"]+`" class="details_section">
		  	<table class="table table-borderless table-dark">
			  <thead>
			  	<tr></tr>
			  </thead>
			  <tbody id="_tbody_`+json_obj["id"]+`">

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
	$.get("/list/catalogue/resource-item", function(data) {
            // $("#searched_items").text(data);
            data=JSON.parse(data)
            for (var i = 0; i < data.length; i++) {                
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
	tags_set=seen_tags_set;

	$("#landing_footer, #normal_footer").html(getFooterContent()	);
	$.get("/count/catalogue/attribute?item-type=resource-item", function(data) {
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
console.log( this.value );
});




// Capture search input click
$(".ui-menu").on('click',function(){
	console.log("s",this.value)
});




/*************************************************EVENT BINDINGS END*********************************************/









/********************************************************************************************/



/********************************************************************************************/

