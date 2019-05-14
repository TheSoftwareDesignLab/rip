var graph1Object;
var orderedImageNames = [];
var folder = document.getElementById("myInput");
var images = [];
var imgDic = {};
var images2 = [];
let dicByImgName = [];

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

    readMeta();
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

        reader.readAsText(logJsonlinks);
    }

    function readTree() {
        var reader = new FileReader();

        // Closure to capture thelinksfile information.
        reader.onload = (function (theFile) {
            return function (e) {
                // Render thumbnail.
                JsonObj = JSON.parse(e.target.result);
                treeJson = JsonObj;
                //graph1Object = transformTreeToD3(treeJson);
                console.log(treeJson.links)
                treeJson.links = treeJson.links.splice(1)
                treeJson.links.forEach(element => {
                    console.log(element)
                    if (!element.name) {
                        if (element.actions) {
                            element.name = element.actions[element.actions.length - 1];
                            element.value = 4;
                        }
                    }
                });
                loadDomainModel(treeJson);
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
                        hAttribute.innerText = attribute.field + ' : ' + attribute.type;
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
                    dicByImgName[parseInt(node.image.split(".")[0], 10) - 1] = node;
                    orderedImageNames[parseInt(node.image.split(".")[0], 10) - 1] = node.image;
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
        document.getElementById("time").innerText = "Execution Time (min): " + meta.elapsedTime;
        document.getElementById("maxTime").innerText = "Max Execution Time (min): " + meta.maxTime;
        document.getElementById("numberOfEvents").innerText = "Number of events: " + meta.execEvents;
        document.getElementById("maxNumberOfEvents").innerText = "Max Number of events: " + meta.maxEvents;
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
        var previousImageId = (parseInt(imageId) - 1)+""
        var nextImageId = (parseInt(imageId) + 1)+""
        // console.log(imageId);

        //Current view
        document.getElementById("currName").innerText = currentView['name'];
        document.getElementById("currBattery").innerText = currentView['battery'] + '%';
        document.getElementById("currWifi").innerText = currentView['wifi'] ? 'ON' : 'OFF';
        document.getElementById("currMemory").innerText = currentView['memory'] + ' kB';
        document.getElementById("currCpu").innerText = currentView['cpu'] + '%';
        document.getElementById("currAirplane").innerText = currentView['airplane'] ? 'ON' : 'OFF';

        //Previous view
        if(parseInt(imageId)>0){
            var prevView = dicByImgName[previousImageId];
            document.getElementById("prevBattery").innerText = prevView['battery'] + '%';
            document.getElementById("prevWifi").innerText = prevView['wifi'] ? 'ON' : 'OFF';
            document.getElementById("prevMemory").innerText = prevView['memory'] + ' kB';
            document.getElementById("prevCpu").innerText = prevView['cpu'] + '%';
            document.getElementById("prevAirplane").innerText = prevView['airplane'] ? 'ON' : 'OFF';
        } else {
            document.getElementById("prevBattery").innerText = "-";
            document.getElementById("prevWifi").innerText = "-";
            document.getElementById("prevMemory").innerText = "-";
            document.getElementById("prevCpu").innerText = "-";
            document.getElementById("prevAirplane").innerText = "-";
        }

        //Next view
        if(parseInt(imageId)<dicByImgName.length-1){
            var nextView = dicByImgName[nextImageId];
            document.getElementById("nextBattery").innerText = nextView['battery'] + '%';
            document.getElementById("nextWifi").innerText = nextView['wifi'] ? 'ON' : 'OFF';
            document.getElementById("nextMemory").innerText = nextView['memory'] + ' kB';
            document.getElementById("nextCpu").innerText = nextView['cpu'] + '%';
            document.getElementById("nextAirplane").innerText = nextView['airplane'] ? 'ON' : 'OFF';
        } else {
            document.getElementById("nextBattery").innerText = "-";
            document.getElementById("nextWifi").innerText = "-";
            document.getElementById("nextMemory").innerText = "-";
            document.getElementById("nextCpu").innerText = "-";
            document.getElementById("nextAirplane").innerText = "-";
        }


    }

    function htmlLoadSequential() {

        var flip = $("#flipster1").flipster({
            buttons: true,
            onItemSwitch: flipsterChanges,
            start: 0
        });
        var ul = document.getElementById("flipsterUL");
        promises = []
        orderedImageNames.forEach(imageName => {
            //console.log(imageName)
            imageName = parseInt(imageName.split(".")[0], 10) - 1
            //console.log('FileName', imageName)
            let file = imgDic[(imageName + 1) + ".png"];
            //console.log('FILE', file);

            async function readFileAsDataURL(file) {
                promises[imageName] = new Promise((resolve) => {
                    let fileReader = new FileReader();
                    fileReader.onload = (e) => resolve(fileReader.result);
                    fileReader.readAsDataURL(file);
                });
            }
            readFileAsDataURL(file)
        });
        Promise.all(promises).then(values => {
            // console.log(values)
            values.forEach((element,ind) => {
                var li = document.createElement("li");
                var imgHTML = document.createElement("img");
                imgHTML.src = element;
                imgHTML.height = "500";
                imgHTML.setAttribute("id", ind);
                $(li).append(imgHTML);
                $(ul).append(li);
                // console.log(ul)
                flip.flipster('index');
            });
        })

        if (document.getElementById("dumy1")) {
            document.getElementById("dumy1").remove();
            flip.flipster('index');
            document.getElementById("dumy2").remove();
            flip.flipster('index');
        }
    }
}