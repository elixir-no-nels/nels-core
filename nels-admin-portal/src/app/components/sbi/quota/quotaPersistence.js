'use strict';
app.service('quotaPersistenceService', function ($injector, $q, $log,
                                                 localStorageService, NeLSAppSettings, $http, paginationService) {

    var self = this;
    self.blockQuota = 0;

    this.loadSbiQuotas = function (offset, sizePerPage, sort) {
        var uri = NeLSAppSettings.apiUrl + "sbi/quotas/all?limit=" + sizePerPage + "&offset=" + offset + "&sort=" + sort;
        return $http.get(uri).then(function (results) {
            return results.data;
        });
    };

    this.loadQuota = function (quotaId, successCallback, errorCallback) {
        var uri = NeLSAppSettings.apiUrl + "sbi/quotas/" + quotaId;
        $http.get(uri).then(successCallback, errorCallback);
    };

    this.loadBlockQuota = function () {
        var uri = NeLSAppSettings.apiUrl + "sbi/blockquota";
        $http.get(uri).then(function (response) {
            self.blockQuota = response.data;
        });
    };

    this.loadProjectsInQuota = function (quotaId, offset, sort, successCallback, errorCallback) {
        var uri = NeLSAppSettings.apiUrl + "sbi/quotas/" + quotaId + "/projects?limit=" + paginationService.getSizePerPage() + "&offset=" + offset + "&sort=" + sort;
        $http.get(uri).then(successCallback, errorCallback);
    };

    this.createQuota = function (body, successCallback, errorCallback) {
        var uri = NeLSAppSettings.apiUrl + "sbi/quotas";
        $http.post(uri, body).then(successCallback, errorCallback);
    };

    this.updateQuota = function (quotaId, body, successCallback, errorCallback) {
        var uri = NeLSAppSettings.apiUrl + "sbi/quotas/" + quotaId;
        $http.put(uri, body).then(successCallback, errorCallback);
    };

    this.updateBlockQuota = function (body, successCallback, errorCallback) {
        var uri = NeLSAppSettings.apiUrl + "sbi/blockquota";
        $http.post(uri, body).then(successCallback, errorCallback);
    };

    this.deleteQuota = function (quotaId, successCallback, errorCallback) {
        var uri = NeLSAppSettings.apiUrl + "sbi/quotas/" + quotaId;
        $http.delete(uri).then(successCallback, errorCallback);
    };

    this.countQuotas = function () {
        //var uri = NeLSAppSettings.apiUrl + "sbi/quotas/count";
        var uri = NeLSAppSettings.apiUrl + "sbi/quotas/all?limit=0&offset=0&sort=id";
        return $http.get(uri).then(function (results) {
            return results.data.count;
        });
    };

    this.quotaSizes = function () {
        var limit = 5000000;
        var uri = NeLSAppSettings.apiUrl + "sbi/quotas/all?limit=" + limit + "&offset=0";
        return $http.get(uri).then(function (results) {
            var allocated = 0, used = 0;
            results.data.data.forEach(function (quota) {
                allocated += quota.quota_size;
                used += quota.used_size;
            });

            return {allocated: allocated, used: used};
        });
    };

    this.searchQuotas = function (searchText, offset, sort) {
        var deferred = $q.defer();
        var uri = NeLSAppSettings.apiUrl + "sbi/quotas/query?limit=" + paginationService.getSizePerPage() + "&offset=" + offset + "&sort=" + sort;
        var params = {"query": searchText};
        $http.post(uri, params).success(
            function (result) {
                //post processing here
                deferred.resolve(result);

            }).error(function (msg, code) {
            //error processing
            deferred.reject(msg);

        });

        return deferred.promise;

    };

});