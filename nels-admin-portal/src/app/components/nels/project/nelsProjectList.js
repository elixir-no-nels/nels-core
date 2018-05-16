'use strict';

app.controller('nelsProjectListController', function ($scope, $location, $log,
                                                      NeLSAppSettings, nelsAuthService, nelsNavigatorService,
                                                      nelsProjectPersistenceService, selectionService, nelsUtilsService, nelsAlertService, paginationService) {
    nelsNavigatorService.requireLogin();
    var self = this;

    self.sortColumn = 'id';
    self.reverse = true;
    self.searchText = '';
    self.selectedProjects = [];

    this.$onInit = function () {
        paginationService.init();
        paginationService.setNumberOfItemsPerPage(NeLSAppSettings.numberOfItemsPerPage);

        self.loadProjects();
    };

    self.sortBy = function (sortColumn) {
        paginationService.init();
        self.reverse = (self.sortColumn === sortColumn) ? !self.reverse
            : false;
        self.sortColumn = sortColumn;
        self.sortProjects();
    };

    self.sortProjects = function () {
        selectionService.clearSelection(self.selectedProjects);

        if (self.searchText) {
            self.searchProjects();
        } else {
            var sort;
            if (self.reverse) {
                sort = "-" + self.sortColumn;
            } else {
                sort = self.sortColumn;
            }
            nelsProjectPersistenceService.loadProjects(paginationService.offset, sort).then(function (results) {
                self.projects = results.data;
                paginationService.totalSize = results.count;
                paginationService.totalPage = Math.floor((paginationService.totalSize + paginationService.numberOfItemsPerPage - 1) / paginationService.numberOfItemsPerPage);

            });
        }
    };

    self.searchProjects = function () {
        var sort;
        if (self.reverse) {
            sort = "-" + self.sortColumn;
        } else {
            sort = self.sortColumn;
        }
        nelsProjectPersistenceService.searchProjects(self.searchText, paginationService.offset, sort)
            .then(function (results) {
                self.projects = results.data;

                paginationService.totalSize = results.count;
                paginationService.totalPage = Math.floor((paginationService.totalSize + paginationService.numberOfItemsPerPage - 1) / paginationService.numberOfItemsPerPage);

            });
    };

    self.loadProjects = function () {
        var sort;
        if (self.reverse) {
            sort = "-" + self.sortColumn;
        } else {
            sort = self.sortColumn;
        }
        nelsProjectPersistenceService
            .loadProjects(paginationService.offset, sort)
            .then(
                function (results) {
                    self.projects = results.data;
                    paginationService.totalSize = results.count;
                    paginationService.totalPage = Math.floor((paginationService.totalSize + paginationService.numberOfItemsPerPage - 1) / paginationService.numberOfItemsPerPage);

                });
    };


    self.search = function () {
        selectionService.clearSelection(self.selectedProjects);
        paginationService.init();

        self.searchProjects();

    };

    self.add = function () {
        //TODO create new project
    };

    self.getTotalSize = function () {
        return paginationService.totalSize;
    };

    /* paging */
    self.getPages = function () {
        return paginationService.getPages();
    };

    self.setCurrentPage = function (page) {
        paginationService.setCurrentPage(page);
        selectionService.clearSelection(self.selectedProjects);
        if (self.searchText) {
            self.searchProjects();
        } else {
            self.loadProjects();
        }
    };

    self.getPageClass = function (page) {
        return paginationService.getPageClass(page);
    };

    self.getPreviousClass = function () {
        return paginationService.getPreviousClass();
    };

    self.previousPage = function () {
        if (paginationService.pages[0] != 1) {
            paginationService.previousPage();
            selectionService.clearSelection(self.selectedProjects);
            if (self.searchText) {
                self.searchProjects();
            } else {
                self.loadProjects();
            }
        }
    };

    self.firstPage = function () {
        if (paginationService.pages[0] != 1) {
            selectionService.clearSelection(self.selectedProjects);
            paginationService.firstPage();
            if (self.searchText) {
                self.searchProjects();
            } else {
                self.loadProjects();
            }
        }
    };

    self.getNextClass = function () {
        return paginationService.getNextClass();
    };

    self.nextPage = function () {
        if (paginationService.pages[paginationService.pages.length - 1] != paginationService.totalPage) {
            paginationService.nextPage();
            selectionService.clearSelection(self.selectedProjects);
            if (self.searchText) {
                self.searchProjects();
            } else {
                self.loadProjects();
            }
        }
    };

    self.lastPage = function () {
        if (paginationService.pages[paginationService.pages.length - 1] != paginationService.totalPage) {
            paginationService.lastPage();
            selectionService.clearSelection(self.selectedProjects);
            if (self.searchText) {
                self.searchProjects();
            } else {
                self.loadProjects();
            }
        }
    };

    self.change = function (numberPerPage) {
        selectionService.clearSelection(self.selectedProjects);
        paginationService.change(numberPerPage);
        if (self.searchText) {
            self.searchProjects();
        } else {
            self.loadProjects();
        }
    };

    self.startNumber = function () {
        return paginationService.startNumber();
    };

    self.endNumber = function () {
        return paginationService.endNumber();
    };

    self.getSearchClass = function () {
        return "";
    };

    self.getAddClass = function () {
        return "invisible";
    };

    self.getSelectionClass = function () {
        return "invisible";
    };

    selectionService.clearSelection(self.selectedProjects);

    // export services
    $scope.nelsAlertService = nelsAlertService;
    $scope.selectionService = selectionService;
});