'use strict';

app.service('nelsUtilsService', function ($http, $log, $q) {
    var self = this;

    this.appendWithDelimiter = function (src, toAdd, delimiter) {
        return (src == null || src == "") ? toAdd : src + delimiter + toAdd;
    }

    this.range = function (lowEnd, highEnd) {
        var arr = [],
            c = highEnd - lowEnd + 1;
        while (c--) {
            arr[c] = highEnd--
        }
        return arr;
    }

    this.checkIfExisting = function (item, array) {
        for (var i = 0; i < array.length; i++) {
            if (array[i] === item) {
                return true;
            }
        }
        return false;
    };


});

app.service('paginationService', function (nelsUtilsService, NeLSAppSettings, $log) {
    var self = this;


    self.defaultLengthForPages = NeLSAppSettings.defaultLengthForPages;
    self.numberOfItemsPerPage = NeLSAppSettings.numberOfItemsPerPage;
    self.offset = 0;
    self.currentPage = Math.floor(self.offset / self.numberOfItemsPerPage) + 1;
    self.pages = [];


    self.init = function () {
        self.pages = [];
        self.offset = 0;
        self.currentPage = 1;
    };

    self.change = function (size) {
        self.numberOfItemsPerPage = size;
        self.offset = 0;
        self.currentPage = 1;
    };

    self.setNumberOfItemsPerPage = function (size) {
        self.numberOfItemsPerPage = size;
    };

    self.getSizePerPage = function () {
        return self.numberOfItemsPerPage;
    };

    self.getPages = function () {
        if (self.currentPage == 1) {
            if (self.totalPage > self.defaultLengthForPages) {
                self.pages = nelsUtilsService.range(1, self.defaultLengthForPages);
            } else {
                self.pages = nelsUtilsService.range(1, self.totalPage);
            }
            return self.pages;
        } else {
            if (nelsUtilsService.checkIfExisting(self.currentPage, self.pages)) {
                return self.pages;
            } else {
                if (self.currentPage == (self.pages[0] - 1)) {
                    self.pages = nelsUtilsService.range(self.pages[0] - self.defaultLengthForPages, self.pages[0] - 1);
                    return self.pages;
                }
                if (self.currentPage == (self.pages[self.pages.length - 1] + 1)) {
                    var start = self.pages[self.pages.length - 1] + 1;
                    if ((start + self.defaultLengthForPages - 1) < self.totalPage) {
                        self.pages = nelsUtilsService.range(start, start + self.defaultLengthForPages - 1);
                    } else {
                        self.pages = nelsUtilsService.range(start, self.totalPage);
                    }
                    return self.pages;
                }
                if (self.currentPage == self.totalPage) {
                    var temp = self.totalPage % self.defaultLengthForPages;
                    if (temp == 0) {
                        self.pages = nelsUtilsService.range(self.totalPage - self.defaultLengthForPages + 1, self.totalPage);
                    } else {
                        self.pages = nelsUtilsService.range(self.totalPage - temp + 1, self.totalPage);
                    }
                    return self.pages;
                }
            }
        }
    };

    self.setCurrentPage = function (page) {
        self.currentPage = page;
        $log.info("current page in setCurrentPage is " + self.currentPage);
        self.offset = (self.currentPage - 1) * self.numberOfItemsPerPage;
    };

    self.getPageClass = function (page) {
        return (page === self.currentPage) ? "active" : "";
    };

    self.getPreviousClass = function () {
        return self.pages[0] == 1 ? "disabled" : "nels-clickable";
    };

    self.firstPage = function () {
        self.currentPage = 1;
        self.offset = 0;
    };

    self.previousPage = function () {
        self.currentPage = self.pages[0] - 1;
        self.offset = (self.currentPage - 1) * self.numberOfItemsPerPage;
    };

    self.getNextClass = function () {
        return self.pages[self.pages.length - 1] == self.totalPage ? "disabled" : "nels-clickable";
    };

    self.nextPage = function () {
        self.currentPage = self.pages[self.pages.length - 1] + 1;
        self.offset = (self.currentPage - 1) * self.numberOfItemsPerPage;

    };

    self.lastPage = function () {
        self.currentPage = self.totalPage;
        self.offset = (self.totalPage - 1) * self.numberOfItemsPerPage;
    };

    self.startNumber = function () {
        return self.numberOfItemsPerPage * self.currentPage - self.numberOfItemsPerPage + 1;
    };

    self.endNumber = function () {
        var endNumber = self.numberOfItemsPerPage * self.currentPage;
        return self.totalSize < endNumber ? self.totalSize : endNumber;
    };
});

app.service('selectionService', function ($log) {
    var self = this;

    this.selectionCss = function (selectedItems, item) {
        return self.hasItem(selectedItems, item) ? 'selected-item' : '';
    };

    this.clearSelection = function (selectedItems) {
        selectedItems.splice(0);
    };

    this.selectAll = function (items, selectedItems) {
        angular.forEach(items, function (item) {
            if (!self.hasItem(selectedItems, item)) {
                selectedItems.push(item);
            }
        });
    };
    this.hasItem = function (items, item) {
        return items.indexOf(item) >= 0;
    };
    this.removeItem = function (items, item) {
        var index = items.indexOf(item);
        if (index >= 0) {
            items.splice(index, 1);
        }
    };

    this.toggleItem = function (selectedItems, item) {
        if (self.hasItem(selectedItems, item)) {
            self.removeItem(selectedItems, item);
        } else {
            selectedItems.push(item);
        }
    };

});

/* auth service */
app.service('nelsAuthService', function ($injector, $q, $http, $window, localStorageService,
                                         NeLSAppSettings, $log) {

    var self = this;

    this.auth = {
        isAuth: false,
        name: "",
        nelsId: -1,
        userType: ""
    };

    this.getAuthorizationDataKey = function () {
        return NeLSAppSettings.apiUrl + ".authorizationData";
    };

    this.login = function (access_token) {

        var deferred = $q.defer();
        var url = NeLSAppSettings.apiUrl + "user-info";
        $http.get(NeLSAppSettings.apiUrl + "user-info", {
            headers: {
                'Authorization': 'Bearer ' + access_token
            }
        }).then(function (response) {
            localStorageService.set(self.getAuthorizationDataKey(), {
                token: access_token,
                nelsId: response.data.nels_id,
                name: response.data.name,
                userType: response.data.user_type
            });
            self.auth.isAuth = true;
            self.auth.nelsId = response.data.nels_id;
            self.auth.name = response.data.name;
            self.auth.userType = response.data.user_type;
            _paq.push(['setUserId', self.auth.name]);
            _paq.push(['trackPageView']);
            deferred.resolve(response);

        }, function (response) {
            $log.error(response);
            self.logOut();
            deferred.reject(err);
        });

        return deferred.promise;

    };

    this.logOut = function () {
        localStorageService.remove(self.getAuthorizationDataKey());
        self.auth.isAuth = false;
        self.auth.nelsId = -1;
        self.auth.name = "";
        self.auth.userType = "";

        _paq.push(['resetUserId']);
        _paq.push(['trackPageView']);

        //go to logout url of portal
        $window.location.href = NeLSAppSettings.logoutUrl;
        /*
        $http.get(NeLSAppSettings.logoutUrl).then(function (response) {
            $log.info(response);
        }, function (response) {
            $log.error(response);
        });*/
    };

    this.loadAuthData = function () {
        var authData = localStorageService.get(self.getAuthorizationDataKey());
        if (authData) {
            self.auth.isAuth = true;
            self.auth.name = authData.name;
            self.auth.nelsId = authData.nelsId;
            self.auth.userType = authData.userType
        }
    };
});

/* auth intercept service */
app.service('authInterceptorService', function ($q, $injector, $location,
                                                localStorageService, $log) {
    var self = this;
    var $http;

    this.request = function (config) {
        config.headers = config.headers || {};
        var nelsAuthService = $injector.get('nelsAuthService');
        var authData = localStorageService.get(nelsAuthService.getAuthorizationDataKey());
        if (authData) {
            config.headers.Authorization = 'Bearer ' + authData.token;
        }
        return config;
    };

    this.responseError = function (rejection) {
        var deferred = $q.defer();
        if (rejection.status == 401) {
            var nelsAuthService = $injector.get('nelsAuthService');
            nelsAuthService.logOut();
            $location.path('/');
            deferred.reject(rejection);
        } else if (rejection.status == 403) {
            $location.path('/denied');
            deferred.reject(rejection);
        } else {
            deferred.reject(rejection);
        }
        return deferred.promise;
    }

});

app.service('nelsNavigatorService', function ($injector, $q, $state,
                                              localStorageService, NeLSAppSettings, $log, $window, $location,
                                              nelsAuthService) {

    var self = this;

    this.goToUrl = function (url) {
        $window.location.href = url;
    };

    this.goHome = function () {
        if (nelsAuthService.auth.isAuth) {
            $location.hash(null);
            $location.path('/dashboard');
        }
        else {
            self.goToLoginPage();
        }
    };

    this.goToLoginPage = function () {
        $location.hash(null);
        $location.path('/');
    };

    this.requireLogin = function () {
        if (!nelsAuthService.auth.isAuth) {
            self.goToLoginPage();
        }
    };

    this.goToProjectDetailPage = function (projectId) {
        $location.hash(null);
        $location.path('/sbiProjects/' + projectId);
    }
});



/* enum service */
app.service('enumService', function ($log) {
    var self = this;

    self.invalidNumber = -1;
});

app.filter('bytes', function () {
    return function (bytes, precision) {
        if (bytes === 0) {
            return '0 bytes'
        }
        if (isNaN(parseFloat(bytes)) || !isFinite(bytes)) return bytes;
        if (typeof precision === 'undefined') precision = 1;

        var units = ['bytes', 'kB', 'MB', 'GB', 'TB', 'PB'],
            number = Math.floor(Math.log(bytes) / Math.log(1024)),
            val = (bytes / Math.pow(1024, Math.floor(number))).toFixed(precision);


        return (val.match(/\.0*$/) ? val.substr(0, val.indexOf('.')) : val) + ' ' + units[number];
    }
});

app.filter('magnitude', function () {
    return function (size, unit) {
        var units = {
            "bytes": 1,
            "kB": 1024,
            "MB": 1024 * 1024,
            "GB": 1024 * 1024 * 1024,
            "TB": 1024 * 1024 * 1024 * 1024,
            "PB": 1024 * 1024 * 1024 * 1024 * 1024
        };
        return size * units[unit];

    }
});

app.filter('highlight', function ($sce) {
    return function (text, phrase) {
        if (phrase) text = text.replace(new RegExp('(' + phrase + ')', 'gi'),
            '<span class="nels-highlight">$1</span>')
        return $sce.trustAsHtml(text)
    }
});
