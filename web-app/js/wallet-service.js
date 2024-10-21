function getWalletBalance(userId){
    const response = GET('https://reqres.in/api/users?page=2', {});
    return response;
}

function GET(url, headers){
    var response;
    $.ajax({
         url: url,
         beforeSend: function(xhr){ },
         async: false,
         type: "GET",
         success: function(data) {
             response = $(data);
         }
     });
}