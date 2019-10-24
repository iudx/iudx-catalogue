function get_conf(__x_name,__y_name){
	return {
		type: 'line',
		// data: {
		// 	labels: __x,
		// 	datasets: [{
		// 		label: app_conf['y_axis_parameter'],
		// 		data: __y,
		// 		borderColor: window.chartColors.red,
		// 		backgroundColor: 'rgba(0, 0, 0, 0)',
		// 		fill: false,
		// 		cubicInterpolationMode: 'monotone'
		// 	}
		// 	// , {
		// 	// 	label: 'Cubic interpolation (default)',
		// 	// 	data: datapoints,
		// 	// 	borderColor: window.chartColors.blue,
		// 	// 	backgroundColor: 'rgba(0, 0, 0, 0)',
		// 	// 	fill: false,
		// 	// }, {
		// 	// 	label: 'Linear interpolation',
		// 	// 	data: datapoints,
		// 	// 	borderColor: window.chartColors.green,
		// 	// 	backgroundColor: 'rgba(0, 0, 0, 0)',
		// 	// 	fill: false,
		// 	// 	lineTension: 0
		// 	// }
		// 	]
		// },
		options: {
			responsive: true,
			title: {
				display: true,
				text: "Temporal trend"
			},
			tooltips: {
				mode: 'index'
			},
			scales: {
				xAxes: [{
					display: true,
					scaleLabel: {
						display: true,
						labelString: __x_name
					}
				}],
				yAxes: [{
					display: true,
					scaleLabel: {
						display: true,
						labelString: __y_name
					}
					// ,
					// ticks: {
					// 	suggestedMin: app_conf['suggested_min_value'],
					// 	suggestedMax: app_conf['suggested_min_value'],
					// }
				}]
			}
		}
	}
}