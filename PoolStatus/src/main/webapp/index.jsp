<html>
<head>
    <script type="text/javascript" src="https://www.gstatic.com/charts/loader.js"></script>
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.2.1/jquery.min.js"></script>
    <script type="text/javascript">

        var chartColNames;
        var chartData;
        var selectedSets = new Array();
        var allFileSystems;

        $( document ).ready(function() {
            google.charts.load('current', {'packages':['corechart']});
            $.getJSON("/PoolStatus/rest/status/allsets",
                function(allsets) {
                    allFileSystems = new Array();
                    for (s in allsets) {
                        if (allsets[s].indexOf('/') != -1 || allsets[s] == "scrub") {
                            selectedSets.push(allsets[s]);
                            allFileSystems.push(allsets[s]);
                            addCheckbox(allsets[s]);
                        }
                    }

                    addButtons();

                    fetchAndDraw();
                });
        });

        function fetchAndDraw() {
            var queryString = "?"
            for (ss in selectedSets) {
                queryString=queryString+"filesystem="+selectedSets[ss]+"&";
            }
            chartColNames = selectedSets;
            $.getJSON("/PoolStatus/rest/status/useddata"+queryString ,
                function(data) {
                    // set data, auto date conversion would be great but apparently it is awkward
                    chartData = new Array(data.length);
                    for (i = 0; i < data.length; i++) {
                        chartData[i] = new Array(data[i].length);
                        chartData[i][0] = new Date(data[i][0]);
                        for (j = 1; j < data[i].length; j++){
                            chartData[i][j] = data[i][j];

                        }
                    }
                    google.charts.setOnLoadCallback(drawChart);
                });

        }

        function addButtons() {
            var container = $('#sets');
            $('<input />', { type: 'button', id: name, style: 'width: 30px;', value: 'all',  click: checkAll }).appendTo(container);
            $('<input />', { type: 'button', id: name, style: 'width: 45px;', value: 'none', click: checkNone }).appendTo(container);

        }

        function addCheckbox(name) {
            var container = $('#sets');

            $('<input />', { type: 'checkbox', id: name, style: 'width: 30px;', value: name, checked: true, click: fileSystemClick }).appendTo(container);
            $('<label />', { 'for': name, text: name, style: ' display:inline-block; width: 160px'}).appendTo(container);
        }

        function fileSystemClick() {
            selectedSets = new Array();
            $('#sets input').each(function(index,cb) {
                if (cb.checked ) {
                    selectedSets.push(cb.value);
                }
            });
            fetchAndDraw();
        }

        function doAll(state) {
            $('#sets input').each(function(index,cb) {
                cb.checked = state;
            });
            fetchAndDraw();
        }

        function checkNone() {
            selectedSets = new Array();
            doAll(false);
        }

        function checkAll() {
            selectedSets = allFileSystems.slice(0);
            doAll(true);
        }

        function drawChart() {
            if (selectedSets.length == 0) {
                $('#chart_div').hide();
                return;
            }
            $('#chart_div').show();

            var dataTable = new google.visualization.DataTable(); // google.visualization.arrayToDataTable(chartData);

            dataTable.addColumn('datetime', 'Date');
            for (setName in chartColNames) {
                if (chartColNames[setName].indexOf('/') != -1 || chartColNames[setName] == "scrub") {
                    dataTable.addColumn('number', chartColNames[setName]);
                }
            }

            dataTable.addRows(chartData);


            var options = {
                title: 'Pool Use',
                pointsVisible: "true",
                isStacked: "true",
                chartArea: {left:50,top:20,width:'80%',height:'90%'},
                hAxis: {title: 'Date',  titleTextStyle: {color: '#333'}, format: 'M/d' },
                vAxis: {minValue: 0, format: 'short'},
                interpolateNulls : "true"
            };

            var chart = new google.visualization.AreaChart(document.getElementById('chart_div'));
            chart.draw(dataTable, options);
        }
    </script>
</head>
<body>
<div id="sets" style="float:left; width: 200px">
    <h2>File Systems</h2>
</div>
<div id="chart_div" style="float: left; width: 1200px; height: 900px;"></div>
</body>
</html>
