            var legend = L.control({position: 'bottomright'});
            legend.onAdd = function (map) {

            var div = L.DomUtil.create('div', 'info legend');
            labels = ['<strong>Categories</strong>'],
            categories = localStorage.getItem("resourceClass").split(',')
            // console.log(categories)
            var colors = localStorage.getItem("resourceClassColors").split(',');
            for (var i = 0; i < categories.length; i++) {

                    div.innerHTML += 
                    labels.push(
                        '<span class="dots" style="background:' + colors[i] + '"></span> ' +
                    (categories[i] ? categories[i] : '+'));

                }
                div.innerHTML = labels.join('<br>');
            return div;
            };
            legend.addTo(map);