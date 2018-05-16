'use strict';

app.service('dashboardPersistenceService', function ($http, NeLSAppSettings) {

    var self = this;

    this.loadDashboardInfo = function () {
        var uri = NeLSAppSettings.apiUrl + "dashboard/";
        return $http.get(uri);
    };


});