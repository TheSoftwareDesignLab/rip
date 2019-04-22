var graph1Object;
var orderedImageNames = [];
var folder = document.getElementById("myInput");
var images = [];
var imgDic = {};
var images2 = [];
let dicByImgName = {};

let metaJson = {};
let treeJson = {};
let logJson = {};
let sequantialJson = {};
var thing = {};


$('#loadProject').click(loadNewProject);

function loadNewProject() {
    var confirm = window.confirm("Are you sure?");

    if (confirm) {
        window.location.reload(false);
    }
}

/*
Main function that detects file load
*/
folder.onchange = function () {
    var files = folder.files,
        len = files.length,
        i;
    //console.log('files', files)
    for (i = 0; i < len; i += 1) {
        //console.log(files[i]);

        if (files[i].type == "image/png") {
            images.push(files[i]);
            imgDic[files[i].name] = files[i];
        }

        if (files[i].name == "meta.json") {
            metaJson = files[i];
        }

        if (files[i].name == "tree.json") {
            treeJson = files[i];
        }

        if (files[i].name == "log.log") {
            logJson = files[i];
        }

        if (files[i].name == 'sequential.json') {
            sequantialJson = files[i];
        }
    }

    // readMeta();
    // readLog();
    readSequential();
    readTree();

    function readMeta() {
        var reader = new FileReader();
        // Closure to capture the file information.
        reader.onload = (function (theFile) {
            return function (e) {
                // Render thumbnail.
                JsonObj = JSON.parse(e.target.result);
                htmlLoadMeta(JsonObj);
            };
        })(metaJson);

        reader.readAsText(metaJson);
    }

    function readLog() {
        var reader = new FileReader();

        // Closure to capture the file information.
        reader.onload = (function (theFile) {
            return function (e) {
                // Render thumbnail.
                var logText = e.target.result;
                document.getElementById("logCode").innerHTML = logText;

            };
        })(logJson);

        reader.readAsText(logJson);
    }

    function readTree() {
        var reader = new FileReader();

        // Closure to capture the file information.
        reader.onload = (function (theFile) {
            return function (e) {
                // Render thumbnail.
                JsonObj = JSON.parse(e.target.result);
                treeJson = JsonObj;
                //graph1Object = transformTreeToD3(treeJson);
                treeJson.links.forEach(element => {
                    if (!element.name) {
                        if (element.actions) {
                            element.name = element.actions[element.actions.length - 1];
                            element.value = 4;
                        }
                    }
                });
                // loadDomainModel(treeJson);
                drawGraph1(treeJson);
            };
        })(treeJson);

        reader.readAsText(treeJson);
    }

    function loadTransitions() {

    }

    function loadDomainModel(treeJsonFile) {
        var addedAct = [];
        treeJsonFile.nodes.forEach(element => {
            if (element['model'] && element['model'].length > 0) {
                var name = element['activityName'];
                if (!name) {
                    name = element['name'];
                }
                if (addedAct.includes(name)) {
                    console.log("The activity name is already included")
                } else {
                    addedAct.push(name);

                   // console.log("added acts", addedAct);

                    var nDiv = document.createElement('div');
                    nDiv.classList.add('col-md-3');
                    var divPanel = document.createElement('div');
                    divPanel.classList.add('panel', 'panel-primary');
                    var divHeading = document.createElement('div');
                    divHeading.classList.add('panel-heading');
                    divHeading.innerHTML = name

                    var divContent = document.createElement('div');
                    divContent.classList.add('panel-body');
                    element.model.forEach(attribute => {
                        var hAttribute = document.createElement('h5');
                        hAttribute.innerText = attribute.name + ' : ' + attribute.type;
                        divContent.appendChild(hAttribute);
                    });


                    nDiv.appendChild(divPanel);
                    divPanel.appendChild(divHeading);
                    divPanel.appendChild(divContent);
                    document.getElementById('domainModelDiv').appendChild(nDiv);
                }
            }
        });
    }

    function transformTreeToD3(tree) {
        var nObj = {
            links: [],
            nodes: []
        };

        tree.links.forEach(element => {
            var newlink = {
                source: element.source,
                target: element.target,
                value: element.value,
                sourceObject: element.source,
                targetObject: element.target
            }
            nObj.links.push(newlink);
        });

        tree.nodes.forEach(element => {
            var newNode = {
                name: element.focusedApp,
                group: element.id,
                fullObject: element
            }
            nObj.nodes.push(newNode);
        });
        return nObj;
    }

    function readSequential() {
        var reader = new FileReader();

        reader.onload = (theFile => {
            return function (e) {
                seqJson = JSON.parse(e.target.result);
                //console.log(seqJson, 'seqJson');

                for (j = 0; j < seqJson.nodes.length; j += 1) {
                    var node = seqJson.nodes[j];
                    //console.log(node, 'Node');
                    orderedImageNames.push(node.image);
                    dicByImgName[node.image] = node;
                    //var linkimagename = seqJson.links[j].image.replace("./generated/testtt/", "")
                    //pushImage(nodeimagename);
                    //pushImage(linkimagename);
                    //orderedImageNames.push(linkimagename);
                }
                htmlLoadSequential();
            };
        })(sequantialJson);

        reader.readAsText(sequantialJson);
    }

    //var ima = images.sort(function(a, b){return a.name.localeCompare(b.name);});
    document.getElementById("pageContent").style.display = "inline";
    document.getElementById("projectLoader").style.display = "none";

    function htmlLoadMeta(meta) {
        document.getElementById("androidVersion").innerText = meta.androidVersion;
        document.getElementById("deviceResolution").innerText = "Device resolution: " + meta.deviceResolution;
        document.getElementById("deviceDimensions").innerText = "Aspect ratio: " + meta.deviceDimensions;
        document.getElementById("currentOrientation").innerText = "Device orientation: " + meta.currentOrientation;

        var apkSplit = meta.apk.split('/');
        document.getElementById("apkName").innerText = apkSplit[apkSplit.length - 1];
        document.getElementById("executionMethod").innerText = "Execution method: " + meta.executionMethod;
        document.getElementById("projectName").innerText = "Project name: " + meta.projectName;
        document.getElementById("numberOfEvents").innerText = "Number of events: " + meta.numberOfEvents;
        document.getElementById("startingDate").innerText = "Execution started: " + meta.startingDate;
        document.getElementById("finishDate").innerText = "Execution finished: " + meta.finishDate;

    }

    function htmlLoadDynamicGraph(graph) {
        console.log(graph);
    }

    function flipsterChanges(current, previous) {
        thing = current;
        var image = current.getElementsByTagName('img');
        var imageId = image[0].id;
        var currentView = dicByImgName[imageId];
        //console.log(currentView);

        //Current view
        document.getElementById("currName").innerText = currentView['name'];
        document.getElementById("currBattery").innerText = currentView['battery'] + '%';
        document.getElementById("currWifi").innerText = currentView['wifi'] ? 'ON' : 'OFF';
        document.getElementById("currMemory").innerText = currentView['memory'] + ' kB';
        document.getElementById("currCpu").innerText = currentView['cpu'] + '%';
        document.getElementById("currAirplane").innerText = currentView['airplane'] ? 'ON' : 'OFF';;


    }

    function htmlLoadSequential() {

        var flip = $("#flipster1").flipster({
            buttons: true,
            onItemSwitch: flipsterChanges,
            start: 0
        });
        var ul = document.getElementById("flipsterUL");
        orderedImageNames.reverse().forEach(imageName => {
            console.log('FileName', imageName)
            let file = imgDic[imageName];
            console.log('FILE', file);
            var reader = new FileReader();
            reader.onload = (function (theFile) {
                return function (e) {
                    var li = document.createElement("li");
                    var imgHTML = document.createElement("img");
                    imgHTML.src = e.target.result;
                    imgHTML.height = "500";
                    imgHTML.setAttribute("id", imageName);
                    li.appendChild(imgHTML);
                    ul.appendChild(li);
                    flip.flipster('index');
                };
            })(file);

            reader.readAsDataURL(file);
        });

        if (document.getElementById("dumy1")) {
            document.getElementById("dumy1").remove();
            flip.flipster('index');
            document.getElementById("dumy2").remove();
            flip.flipster('index');
        }
    }
}