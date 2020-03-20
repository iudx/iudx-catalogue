hide_delay=750
show_delay=750

function set_data_globally(_data){
	__DATA = _data;
}

function status_ajax_call(__url, __grp){
	return new Promise((resolve, reject) => {
		console.log(__url, __grp)
		$.ajax({
			"url": __url,
			"async": false,
			"method": 'POST',
			"headers": {
				"Content-Type": "application/json"
			},
			"data": JSON.stringify({"options":"status", "group": __grp}),
			success: function (data) {
				resolve(data)
			},
			error: function (error) {
				reject(error)
			},
			timeout: 30000 // sets timeout to 30 seconds
		})
	})
}; 

function get_color_for(status){
	console.log(status)
	switch(status){
		case 'recently-active': return '#ff7315';
		case 'recently-live': return '#ffa41b';
		case 'live': return '#21bf73';
		case 'down': return '#f0134d';
		default: return  stringToColour(resource_id_to_html_id(status));
	}
}

function get_key(dict){
	return Object.keys(dict)[0]
}

function get_value(dict){
	return Object.values(dict)[0]
}

function show_status_for(cls){
	console.log(cls)
	if(cls=="all"){
		// display all
		console.log("display all")
		$(".all").show(show_delay)
	}else{
		// first hide all and then display the specific class
		console.log("hide all")
		$(".all").hide(hide_delay)
		console.log("category")
		$("."+cls).show(show_delay)
	}
}

function get_items(_attr_name,_attr_value){
	if(is_attr_empty(_attr_name,_attr_value)){
		return;
	}

	if(!first_get_item_call_done){
		first_get_item_call_done=true;
		display_search_section();
	}

	var _temp_a_v = _attr_value
	
	var r;

	status_ajax_call(cat_conf['resoure_server_base_URL']+"/search", _attr_value)
		.then(data => {
			var status_set = new Set(); 
			// data = JSON.parse(data)
			console.log(data)
			// data=JSON.parse(data)
			set_data_globally(data);
			$("#retrieved_items_count").html("About " + data.length + " results for " + _temp_a_v 
													  + " (Attribute: " + _attr_name + ") | Go to <a href='/'>List View</a>/<a href='/map'>Map View</a><br><br>"
													  + `<div style="text-align:center"><div id="status_filter" class="btn-group btn-group-lg" role="group" aria-label="Basic example">
													  <button type="button" class="btn btn-secondary" onclick="show_status_for('all')">All</button>
													</div><br><br></div>`);
			$("#searched_items").html(``);
			for (var i = 0; i < data.length; i++) {
				status_set.add(get_value(data[i]))
				r = resource_id_to_html_id(get_value(data[i]))
				
				$( "#" + resource_id_to_html_id(get_value(data[i]))).tooltip( "option", "content", get_key(data[i]) );
				$("#searched_items").append(`<span id="`+ resource_id_to_html_id(get_key(data[i])) 
														+ `" class="status_dot all `+r+`" style="background-color:`
														+ get_color_for(get_value(data[i]))
														+ `"title="ID: `
														+ get_key(data[i]) 
														+ `"></span>`);
			}
			
			Array.from(status_set).sort().forEach(status => {
				r=resource_id_to_html_id(status)
				console.log(r)
				$("#status_filter").append(`<button type="button" class="btn `+ r 
											+`" style="color:white;background-color:`+get_color_for(status)+`" onclick="show_status_for('`+r+`')">`
											+ status +`</button>`)
			});

			$('.all').show(show_delay)

		})
		.catch(error => {
			$("#retrieved_items_count").html("");
			$("#searched_items").html("")
			_alertify("Error!!!", '<pre id="custom_alertbox">: ' + error["statusText"] + '</pre>');
			console.log(error)
		})
		$( "#_value" ).autocomplete({
			source: rsg_set,
			select: function( event, ui ) {
				$(".se-pre-con").fadeIn("slow");
				get_items("resourceServerGroup", ui["item"]['label'])
				$(".se-pre-con").fadeOut("slow");
			}
		});
}

function set_attr_value(__attr_name,__attr_value) {
    // ////console.log("v:",$( "#value" ).is(':visible'))
    // ////console.log("_v:",$( "#_value" ).is(':visible'))
    if($( "#value" ).is(':visible')){
            $( "#value" ).autocomplete({
                source: __attr_value,
                select: function( event, ui ) {
					$(".se-pre-con").fadeIn("slow");
					get_items("resourceServerGroup", ui["item"]['label'])
					$(".se-pre-con").fadeOut("slow");
                }
                // ,
                // select: function (e, ui) {
                //  alert("selected!", e);
                // },
                // change: function (e, ui) {
                //  alert("changed!", e, ui);
                // }
            });
        }

    if($( "#_value" ).is(':visible')){
        $( "#_value" ).autocomplete({
            source: __attr_value,
            select: function( event, ui ) {
				$(".se-pre-con").fadeIn("slow");
				get_items("resourceServerGroup", ui["item"]['label'])
				$(".se-pre-con").fadeOut("slow");
            }
        });
    }
}
