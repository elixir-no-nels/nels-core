'use strict';

app.service('sbiUserPersistenceService', function ($injector, $q,
                                                   localStorageService, NeLSAppSettings, $http, $log) {
    this.searchUsers = function (federatedIds) {
        var deferred = $q.defer();
        var uri = NeLSAppSettings.apiUrl + "sbi/users/query" ;
        var params = {"federated_id": federatedIds};
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

    this.createUser = function (name, email, federatedId) {
        var deferred = $q.defer();
        var uri = NeLSAppSettings.apiUrl + "sbi/users" ;
        var params = {"federated_id": federatedId, "name": name, "email": email};
        $http.post(uri, params).success(
            function(data, status) {
                //post processing here
                var result;
                if (status === 201) {
                    result = true;
                } else {
                    result = false;
                }
                deferred.resolve(result);

            }).error(function(data, status) {
            //error processing
            deferred.reject({status:status,data:data});

        });
        return deferred.promise;
    };



});
