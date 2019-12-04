//General catalogue settings
const cat_conf = {
	"smart_city_iudx_logo":"../assets/img/iudx_pscdcl.png",
	"smart_city_name":"PSCDCL",
	"smart_city_url":"https://punesmartcity.in/",
	"resoure_server_base_URL": "https://pudx.resourceserver.iudx.org.in/resource-server/pscdcl/v1",
	"auth_base_URL": "https://auth.iudx.org.in/auth/v1",
	"api_docs_link": "https://apidocs.iudx.org.in",
    "resource_server_group_head":"urn:iudx-catalogue-pune:" ,
	"provider_head":"urn:iudx-catalogue-pune:" ,
	"map_default_view_lat_lng": [18.5644, 73.7858],
	"map_default_lat_lng_name": "PSCDCL Office",
	"map_default_zoom": 12
}

/* set legend icons
   key-name : resource-server-group name, value : icon url
*/

const legends = {
	"pudx-resource-server/streetlight-feeder-sree" : "https://image.flaticon.com/icons/svg/1245/1245929.svg",
	"pudx-resource-server/aqm-bosch-climo": "https://image.flaticon.com/icons/svg/1808/1808701.svg",
	"pudx-resource-server/flood-sensor": "https://image.flaticon.com/icons/svg/1890/1890123.svg",
	"pudx-resource-server/wifi-hotspot": "https://image.flaticon.com/icons/svg/660/660488.svg",
	"pudx-resource-server/ptz-video camera": "https://image.flaticon.com/icons/svg/1111/1111407.svg",
	"pudx-resource-server/crowd-sourced-changebhai": "https://image.flaticon.com/icons/svg/1200/1200848.svg",
	"pudx-resource-server/changebhai": "https://image.flaticon.com/icons/svg/1200/1200848.svg",
	"pudx.resourceserver.iudx.org.in/safetipin": "https://image.flaticon.com/icons/svg/541/541384.svg",
	"pudx-resource-server/traffic-incidents": "https://image.flaticon.com/icons/svg/401/401434.svg",
	"pudx-resource-server/tomtom": "https://image.flaticon.com/icons/svg/401/401434.svg",
	"pudx-resource-server/itms-mobility": "https://image.flaticon.com/icons/svg/32/32425.svg",
	"pudx-resource-server/pune-iitm-aqi": "https://image.flaticon.com/icons/svg/1684/1684375.svg",
	"pudx-resource-server/pune-iitm-forecast": "https://image.flaticon.com/icons/svg/1213/1213587.svg",
	"pudx-resource-server/pune-itms": "https://image.flaticon.com/icons/svg/32/32425.svg"
}

//set attribution for used icons -> prefer flaticon as it has a wide range of options and icons are appealling
const icon_attribution = {
	"author": [
				{"freepik":"https://www.flaticon.com/authors/freepik"}, 
				{"smashicons":"https://www.flaticon.com/authors/smashicons"}, 
				{"flat-icons":"https://www.flaticon.com/authors/flat-icons"}, 
				{"itim2101":"https://www.flaticon.com/authors/itim2101"}
			],
	"site":"flaticon.com",
	"site_link":"https://flaticon.com"
}
