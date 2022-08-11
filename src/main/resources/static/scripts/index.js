$("#refreshButton").click( function() {
    console.log("refresh button triggered");
    $.ajax({
        url : "/listFiles"
    }).done(function(data) {
        console.dir(data);
        var listHTML = "";
        for(file of data){
            listHTML += `<li>${file.id} : ${file.name}</li>`
        }

        $("#fileListContainer").html(listHTML);
    })
});

// window.onload   = function() {

//     document.getElementById("refreshButton").addEventListener("click", function() {
//         console.log("refresh button triggered");    
//     })

//     $.ajax({
//         url : "/listFiles"
//     }).done(function(data) {
//         console.dir(data);
//         var listHTML = "";
//         for(file of data){
//             listHTML += `<li>${file.id} : ${file.name}</li>`
//         }

//         document.getElementById("fileListContainer").innerHTML = listHTML;
//     })

// }

const refreshButtonHandler = () => {
    console.log("refresh button triggered");
    fetch("https://127.0.0.1.nip.io:8443/listFiles")
    .then(response => response.json())
    .then(data => {
        console.dir(data);
        var listHTML = "";
        for(file of data){
            listHTML += `<li>${file.name}</li>`
        }
        document.getElementById("fileListContainer").innerHTML = listHTML;
    });
}

const sendQueryHandler = () => {

    console.log("send query button triggered");
    var queryTerm = document.getElementById("queryTerm").value;
    
    var URL = "https://127.0.0.1.nip.io:8443/search?q=" + queryTerm;

    fetch(URL)
    .then(response => response.json())
    .then(data => {
        console.dir(data);
        var resultHTML = "";
        for(file of data){
            resultHTML += `<li>${file.name} : <a href="${file.webContentLink}">${file.webContentLink} </a> </li>`;
        }
        document.getElementById("queryResults").innerHTML = resultHTML;
    });

}

window.onload = function() {
    document.getElementById("refreshButton").addEventListener("click",refreshButtonHandler );
    document.getElementById("sendQuery").addEventListener("click",sendQueryHandler );
}