function display_search_section(){
	$("#landing_section").fadeOut(200);
	$("body").css("background-image","none");
	$("#search_section").fadeIn(1500);
	get_items();
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
	$( "#_value" ).autocomplete({
	      source: seen_tags_set
	});

	$( "#value" ).autocomplete({
	      source: seen_tags_set
	});
}


function getFooterContent(){
	return `<p>&copy; 2019 <a href="https://iudx.org.in">IUDX </a> | Read the  <a href="https://docs.google.com/document/d/12kQteMgxINPjZUVaNBqvtEYJEfqDn7r7QWbL74o7wPQ/edit?usp=sharing">Doc</a>.</p>`
}

$(document).ready(function(){
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
	$( "#value" ).autocomplete({
	      source: seen_tags_set
	});

	$("#landing_footer, #normal_footer").html(getFooterContent()	);
});

function json_to_htmlcard(json_obj){

	return `
		<div class="col-12 card-margin-top">
		<div class="card">
		  <h5 class="card-header card-header-color">` + json_obj["NAME"] + `</h5>
		  <div class="card-body">
		    <h5 class="card-title">` + json_obj["itemDescription"] + `</h5>
		    <a href="#" class="btn btn-primary">Details</a>
		    <a href="#" class="btn btn-info">Get APIs</a>
		  </div>
		</div>
		</div>
	`	
}

/********************************************************************************************/

/********************************************************************************************/