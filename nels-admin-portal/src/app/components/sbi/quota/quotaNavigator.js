'use strict';
app.service('quotaNavigatorService', function ($injector, $q, NeLSAppSettings, $http, $log, $location, $state) {
    var self = this;

    self.sbiUrl = "app.sbiQuota";

    this.viewQuotas = function() {
        // reload page even if the user is on the same page
        $state.go(self.sbiUrl, {}, {
            reload : true
        });
    };

    this.viewQuotaDetail = function (url, quota_id) {
        $state.go(url, {quotaId : quota_id}, {reload: true});
    };
});