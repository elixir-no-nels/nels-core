'use strict';
app.service('sbiDataSetTypeNavigatorService', function ($q, $log, $state) {
    var self = this;

    self.sbiUrl = "app.sbiDataSetTypes";

    this.viewDataSetTypes = function() {
        // reload page even if the user is on the same page
        $state.go(self.sbiUrl, {}, {
            reload : true
        });
    };
});