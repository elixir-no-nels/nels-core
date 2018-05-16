'use strict';

app.service('sbiDataSetTypePersistenceService', function ($injector, $q,
                                                       localStorageService, NeLSAppSettings, $http, paginationService) {

    var self = this;

    self.loadDataSetTypes = function (offset, sort) {
        var uri = NeLSAppSettings.apiUrl + "sbi/datasettypes?limit=" + paginationService.getSizePerPage() + "&offset=" + offset + "&sort=" + sort;
        return $http.get(uri).then(function (results) {
            return results.data;
        });
    };

    self.createDataSetType = function (data, successCallback, errorCallback) {
        var uri = NeLSAppSettings.apiUrl + "sbi/datasettypes";
        $http.post(uri, data).then(successCallback, errorCallback);
    };

    self.deleteDataSetType = function (typeId, successCallback, errorCallback) {
        var uri = NeLSAppSettings.apiUrl + "sbi/datasettypes/" + typeId;
        $http.delete(uri).then(successCallback, errorCallback);
    };
    
    self.searchDataSetTypes = function(searchText, offset, sort, successCallback, errorCallback) {
        var uri = NeLSAppSettings.apiUrl + "sbi/datasettypes/query?limit=" + paginationService.getSizePerPage() + "&offset=" + offset + "&sort=" + sort;
        var params = {"query": searchText};
        return $http.post(uri, params).then(function (results) {
            return results.data;
        });
    };
});