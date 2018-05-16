'use strict';
app.service('sbiProjectNavigatorService', function ($q, $log, $state) {
    var self = this;

    self.sbiUrl = "app.sbiProjects";

    this.viewProjects = function() {
        // reload page even if the user is on the same page
        $state.go(self.sbiUrl, {}, {
            reload : true
        });
    };
    
    this.viewProjectDetail = function (url, project_id) {
        $state.go(url, {projectId : project_id}, {reload: true});
    }
});