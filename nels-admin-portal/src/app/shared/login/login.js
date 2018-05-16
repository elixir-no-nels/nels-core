'use strict';
/* login controller */
app.controller('loginController', function ($location, $log, NeLSAppSettings, nelsAuthService, nelsNavigatorService) {

    this.isLoggedIn = false;
    var params = {};
    var keyVal;
    for (keyVal of $location.hash().split("&")) {
        var split = keyVal.split("=");
        if (split.length == 2) {
            params[split[0]] = split[1];
        }
    }

    if (params.access_token != undefined) {
        this.isLoggedIn = true;
        this.accessToken = params.access_token;

        nelsAuthService.login(params.access_token).then(function (response) {
            nelsNavigatorService.goHome();
        }, function (response) {
            $log.error(response);
        });
        //nelsNavigatorService.goHome();
        //this.message = "token: " + params.access_token
    }

    if (nelsAuthService.auth.isAuth) {
        nelsNavigatorService.goHome();
    }

    this.login = function () {
        nelsNavigatorService.goToUrl(NeLSAppSettings.oauthUrl);
    };
});