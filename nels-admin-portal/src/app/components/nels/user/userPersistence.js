'use strict';

app.service('userPersistenceService', function ($injector, $q, $log,
                                                localStorageService, NeLSAppSettings, $http, paginationService) {

    this.loadUsers = function (offset, sizePerPage, sort) {
        var uri = NeLSAppSettings.apiUrl + "nels/users?limit=" + sizePerPage + "&offset=" + offset + "&sort=" + sort;
        return $http.get(uri).then(function (results) {

            return results.data;
        });
    };

    this.countUsers = function(){
        //var uri = NeLSAppSettings.apiUrl + "nels/users/count";
        var uri = NeLSAppSettings.apiUrl + "nels/users?limit=0&offset=0&sort=id";
        return $http.get(uri).then(function (results) {
            return results.data.count;
        });
    };

	this.searchUsers = function(searchText, offset, sort) {
        var deferred = $q.defer();
		var uri = NeLSAppSettings.apiUrl + "nels/users/query?limit=" + paginationService.getSizePerPage() + "&offset=" + offset + "&sort=" + sort;
        var params = {"query": searchText};
        $http.post(uri, params).success(
            function(result) {
                //post processing here
                deferred.resolve(result);

            }).error(function(msg, code) {
                //error processing
                deferred.reject(msg);

            });

        return deferred.promise;

	};

});