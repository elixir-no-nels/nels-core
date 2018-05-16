'use strict';

app.service('nelsProjectNavigatorService', function ($injector, $q, NeLSAppSettings, $http, $log, $location, $state) {
    var self = this;

    self.projectsUrl = "app.nelsprojects";
    this.viewProjects = function() {
        // reload page even if the user is on the same page
        $state.go(self.projectsUrl, {}, {
            reload : true
        });
    };
});