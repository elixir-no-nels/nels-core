'use strict';

app.service('nelsProjectPersistenceService', function ($injector, $q,
                                                       localStorageService, NeLSAppSettings, $http, paginationService) {

    var self = this;

    this.loadProjects = function (offset, sort) {
        var uri = NeLSAppSettings.apiUrl + "nels/projects/all?limit=" + paginationService.getSizePerPage() + "&offset=" + offset + "&sort=" + sort;
        return $http.get(uri).then(function (results) {

            return results.data;
        });
    };
    
    this.searchProjects = function(searchText, offset, sort) {
        var deferred = $q.defer();
        var uri = NeLSAppSettings.apiUrl + "nels/projects/query?limit=" + paginationService.getSizePerPage() + "&offset=" + offset + "&sort=" + sort;
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

    this.countProjects = function(){
        //var uri = NeLSAppSettings.apiUrl + "nels/projects/count";
        var uri = NeLSAppSettings.apiUrl + "nels/projects/all?limit=0&offset=0&sort=id";
        return $http.get(uri).then(function (results) {
            return results.data.count;
        });
    };

});