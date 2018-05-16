'use strict';

app.service('userNavigatorService', function ($injector, $q, NeLSAppSettings, $http, $log, $location, $state) {
    var self = this;

    self.usersUrl = "app.users";

    this.viewUsers = function() {
        // reload page even if the user is on the same page
        $state.go(self.usersUrl, {}, {
            reload : true
        });
    };
});