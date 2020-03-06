var cat_conf = null
var legends = null
var icon_attribution = null

var DEBUG = false;
// ENABLE/DISABLE Console Logs
if(!DEBUG){
  console.log = function() {}
}

function conf_ajax_call(__url){
	return new Promise((resolve, reject) => {
        $.ajax({
            "url": __url,
            "async": false,
            "method": 'GET',
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


conf_ajax_call(window.origin+'/catalogue/internal_apis/getconfig')
	.then(data => {
		data = JSON.parse(data)
		cat_conf = data[0]['configurations']
		legends = data[0]['legends']
		icon_attribution = data[0]['global_configuration']['icon_attribution']
	    // _alertify("Success!!!", '<pre id="custom_alertbox">' + jsonPrettyHighlightToId(data) + '</pre>')
	})
	.catch(error => {
	    // _alertify("Error!!!", '<pre id="custom_alertbox">: ' + error["statusText"] + '</pre>');
	    console.log(error)
	})
