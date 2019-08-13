$(document).ready(function () {

    function chunkedFlow(api, chunkHandler) {
        jsonpipe.flow(api, {
            "delimiter": "", // String. The delimiter separating valid JSON objects; default is "\n\n"
            "onHeaders": function (statusText, headers) {
                // Do something with the headers and the statusText.
            },
            "success": function (data) {
                // Do something with this JSON chunk
                chunkHandler(data)
            },
            "error": function (errorMsg) {
                // Something wrong happened, check the error message
                showError(errorMsg)
            },
            "complete": function (statusText) {
                // Called after success/error, with the XHR status text
                showComplete(api, statusText)
            },
            "timeout": 3600000, // Number. Set a timeout (in milliseconds) for the request
            "method": "GET", // String. The type of request to make (e.g. "POST", "GET", "PUT"); default is "GET"
            "headers": { // Object. An object of additional header key/value pairs to send along with request
                "X-Requested-With": "XMLHttpRequest"
            },
            "data": "", // String. A serialized string to be sent in a POST/PUT request,
            "withCredentials": true // Boolean. Send cookies when making cross-origin requests; default is true
        });
    }

    function showError(err) {
        console.log(err)
        $("#console").append("<div class=\"alert alert-danger\" role=\"alert\">" + err + "</div>");
    }

    function showComplete(api, statusText) {
        $("#console").append("<div class=\"alert alert-success\" role=\"alert\">"
            + "`" + api + "`"
            + " successfully loaded, statusText:" + statusText);
    }

    function addUser(user) {
        var li = ""
        $.each(user, function (k, v) {
            li = li + "<li class=\"list-group-item\">" + v + "</li>";
        });
        $("#users").append("<ul class=\"list-group list-group-horizontal-lg\">" + li + "</ul>")
    }

    function renderUserDocs(userDocs) {
        var rows = ""

        $.each(userDocs, function (_, v) {
            rows = rows + "<tr>" +
                "<td>" + v.user + "</td>" +
                "<td>" + v.docs + "</td>" +
                "</tr>";
        });

        $(".userdocs").replaceWith(
            "<tbody class=\"userdocs\">" +
            rows +
            "</tbody>"
        )

    }

    chunkedFlow('users', addUser)
    chunkedFlow('users/docs', renderUserDocs)

});
