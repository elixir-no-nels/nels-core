'use strict';
app.service('sbiProjectPersistenceService',
    function (NeLSAppSettings, $http, $log, $q, paginationService) {

        var self = this;

        this.loadSbiProjects = function (offset, sort) {
            var uri = NeLSAppSettings.apiUrl
                + "sbi/projects/all?limit=" + paginationService.getSizePerPage() + "&offset=" + offset + "&sort=" + sort;
            return $http.get(uri).then(function (results) {
                return results.data;
            });
        };

        this.searchProjects = function (searchText, offset, sort) {
            var uri = NeLSAppSettings.apiUrl
                + "sbi/projects/all/query?limit=" + paginationService.getSizePerPage() + "&offset=" + offset + "&sort=" + sort;
            var body = {"query": searchText};
            return $http.post(uri, body).then(function (results) {
                return results.data;
            });
        };

        this.loadSbiDatasets = function (projectId) {
            var uri = NeLSAppSettings.apiUrl + "sbi/projects/" + projectId
                + "/datasets";
            return $http.get(uri).then(function (results) {
                return results.data;
            });
        };

        this.loadSbiUsers = function (offset) {
            var uri = NeLSAppSettings.apiUrl + "sbi/users?limit=" + self.defaultPageSize + "&offset=" + offset;
            return $http.get(uri).then(function (results) {
                return results.data;
            });
        };

        this.modifyMembers = function (projectId, requestBody, successCallback, errorCallback) {
            var uri = NeLSAppSettings.apiUrl + "sbi/projects/" + projectId + "/users/do";
            $http.post(uri, requestBody).then(successCallback, errorCallback);
        };

        this.deleteSbiProject = function(projectId, successCallback, errorCallback) {
            var uri = NeLSAppSettings.apiUrl + "sbi/projects/" + projectId;
            $http.delete(uri).then(successCallback, errorCallback);
        };

        this.createSbiProject = function (data, successCallback, errorCallback) {
            var uri = NeLSAppSettings.apiUrl + "sbi/projects";
            $http.post(uri, data).then(successCallback, errorCallback);
        };

        this.updateSbiProject = function (projectId, data, successCallback, errorCallback) {
            var uri = NeLSAppSettings.apiUrl + "sbi/projects/" + projectId;
            $http.put(uri, data).then(successCallback, errorCallback);
        };

        this.loadSbiSubtypes = function (projectId, datasetId) {
            var uri = NeLSAppSettings.apiUrl + "sbi/projects/" + projectId
                + "/datasets/" + datasetId;
            return $http.get(uri).then(function (results) {
                return results.data;
            });
        };

        this.loadSbiProject = function (projectId) {
            var uri = NeLSAppSettings.apiUrl + "sbi/projects/" + projectId;
            return $http.get(uri).then(function (results) {
                return results.data;
            });
        };

        this.loadSbiProjectMembers = function (projectId) {
            var uri = NeLSAppSettings.apiUrl + "sbi/projects/" + projectId + "/users";
            return $http.get(uri).then(function (results) {
                return results.data;
            });
        };

        this.getNeLSLink = function (projectId, datasetId, subtype) {
            var uri = NeLSAppSettings.apiUrl + "sbi/projects/" + projectId
                + "/datasets/" + datasetId + "/do";

            var param = {
                subtype_name: subtype.type,
                method: "get_nels_url"
            };

            var deferred = $q.defer();
            $http.post(uri, param).success(function (response) {
                deferred.resolve({url: response.url, type: subtype.type, size: subtype.size});
            }).error(function (err, status) {
                deferred.reject(err);
            });
            return deferred.promise;
        };

        this.getMetaData = function (projectId, datasetId, subtypeName) {
            var uri = NeLSAppSettings.apiUrl + "sbi/projects/" + projectId + "/datasets/" + datasetId + "/" + subtypeName + "/metadata";
            return $http.get(uri).then(function (results) {
                return results.data;
            });
        };

        this.countSbiProjects = function () {
            //var uri = NeLSAppSettings.apiUrl + "sbi/projects/count";
            var uri = NeLSAppSettings.apiUrl + "sbi/projects/all?limit=0&offset=0&sort=name";
            return $http.get(uri).then(function (results) {
                return results.data.count;
            });
        };

    });