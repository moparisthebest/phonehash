var valid_sha1 = /^[0-9a-fA-F]{40}$/;

function unhash() {
    var hash = $('#hash').val().trim();
    if(valid_sha1.test(hash)) {
        $( "#result" ).html('Please wait, working...');
        $.get( "rest/hashToPhone/" + hash, function( data ) {
            $("#result").html(data);
        }).fail(function( data ) {
            $("#result").html("Unknown Error...");
        });
    } else {
        $( "#result" ).html('sha1 hash must be 40 character hexadecimal');
    }
}

$(document).ready(function () {
    //alert('bob');
    $('#hash').keypress(function (e) {
        if (e.which == 13) { // enter
            unhash();
            return false;
        }
    });
    $('#unhash').click(unhash);
});