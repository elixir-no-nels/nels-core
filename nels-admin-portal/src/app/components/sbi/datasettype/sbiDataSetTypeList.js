'use strict';

app.controller('sbiDataSetTypeListController', function ($scope, $location, $log, $uibModal,
                                                      NeLSAppSettings, nelsAuthService, nelsNavigatorService, nelsConfirmActionService,
                                                      sbiDataSetTypePersistenceService, selectionService, nelsUtilsService, nelsAlertService, paginationService) {
    nelsNavigatorService.requireLogin();
    var self = this;
    self.limitToDescription = 150;

    self.sortColumn = 'name';
    self.reverse = true;
    self.searchText = '';
    self.selectedDatasetTypes = [];

    this.$onInit = function () {
        paginationService.init();
        paginationService.setNumberOfItemsPerPage(NeLSAppSettings.numberOfItemsPerPage);

        self.loadDataSetTypes();
    };

    self.sortBy = function (sortColumn) {
        paginationService.init();
        self.reverse = (self.sortColumn === sortColumn) ? !self.reverse
            : false;
        self.sortColumn = sortColumn;
        self.sortDataSetTypes();
    };

    self.sortDataSetTypes = function () {

        if (self.searchText) {
            self.searchDataSetTypes();
        } else {
            var sort;
            if (self.reverse) {
                sort = "-" + self.sortColumn;
            } else {
                sort = self.sortColumn;
            }
            sbiDataSetTypePersistenceService.loadDataSetTypes(paginationService.offset, sort).then(function (result) {
                self.dataSetTypes = result.data;
                paginationService.totalSize = result.count;
                paginationService.totalPage = Math.floor((paginationService.totalSize + paginationService.numberOfItemsPerPage - 1) / paginationService.numberOfItemsPerPage);
                if (paginationService.totalPage > paginationService.defaultLengthForPages) {
                    paginationService.pages = nelsUtilsService.range(1, paginationService.defaultLengthForPages);
                } else {
                    paginationService.pages = nelsUtilsService.range(1, paginationService.totalPage);
                }
            });
        }
    };

    self.getDescription = function (projectDescription) {
        if (projectDescription.length <= self.limitToDescription) {
            return projectDescription;
        } else {
            return projectDescription.substring(0, self.limitToDescription) + "...";
        }
    };

    self.searchDataSetTypes = function () {
        var sort;
        if (self.reverse) {
            sort = "-" + self.sortColumn;
        } else {
            sort = self.sortColumn;
        }
        sbiDataSetTypePersistenceService.searchDataSetTypes(self.searchText, paginationService.offset, sort).then(function (result) {
            self.dataSetTypes = result.data;

            paginationService.totalSize = result.count;
            paginationService.totalPage = Math.floor((paginationService.totalSize + paginationService.numberOfItemsPerPage - 1) / paginationService.numberOfItemsPerPage);

            if (paginationService.totalPage > paginationService.defaultLengthForPages) {
                paginationService.pages = nelsUtilsService.range(1, paginationService.defaultLengthForPages);
            } else {
                paginationService.pages = nelsUtilsService.range(1, paginationService.totalPage);
            }
        });
    };

    self.loadDataSetTypes = function () {
        $log.info("load data set types");
        var sort;
        if (self.reverse) {
            sort = "-" + self.sortColumn;
        } else {
            sort = self.sortColumn;
        }
        sbiDataSetTypePersistenceService.loadDataSetTypes(paginationService.offset, sort).then(function (result) {
            self.dataSetTypes = result.data;
            paginationService.totalSize = result.count;
            paginationService.totalPage = Math.floor((paginationService.totalSize + paginationService.numberOfItemsPerPage - 1) / paginationService.numberOfItemsPerPage);
            if (!paginationService.pages) {
                if (paginationService.totalPage > paginationService.defaultLengthForPages) {
                    paginationService.pages = nelsUtilsService.range(1, paginationService.defaultLengthForPages);
                } else {
                    paginationService.pages = nelsUtilsService.range(1, paginationService.totalPage);
                }
            }
        });
    };

    self.search = function() {
        paginationService.init();

        self.searchDataSetTypes();

    };

    self.deleteType = function (type) {
        var modalOptions = {
            headerText: 'Confirm Deletion',
            bodyText: 'Are you sure that you want to delete data set type ' + type.name + '?',
            actionButtonText: 'Delete',
            closeButtonText: 'Cancel'
        };

        var modalInstance = nelsConfirmActionService.showConfirmDialog(modalOptions);
        modalInstance.result.then(function (isConfirmed) {
            var name = type.name;
            sbiDataSetTypePersistenceService.deleteDataSetType(type.id, function (successResponse) {
                nelsAlertService.showSuccess(name + " is deleted.");
                self.sortDataSetTypes();
            }, function (errorResponse) {
                if (errorResponse.status == 412) {
                    nelsAlertService.showWarning(errorResponse.data.description);
                } else {
                    nelsAlertService.showDanger(name + " is not deleted.");
                }
            });

        });
    };

    self.add = function () {
        var modal = $uibModal.open({
            animation: true,
            templateUrl: 'app/components/sbi/datasettype/createSbiDataSetTypeModal.html',
            controller: 'createSbiDataSetTypeModalController',
            controllerAs: 'csdstctrl',
            size: 'lg',
            scope: $scope
        });

        modal.result.then(function (response) {
            var adder = nelsAuthService.auth.name;
            var subtype_array = [];
            response.subtypes.trim().split(",").forEach(function (element) {
                subtype_array.push(element.trim());
            });

            sbiDataSetTypePersistenceService.createDataSetType(
                {"name": response.name, "creator": adder, "description": response.description, "subtype": subtype_array},
                function (successResponse) {
                    nelsAlertService.showSuccess("The data set type is created.");
                    self.sortDataSetTypes();
                }, function (errorRespone) {

                });

        });

        modal.closed.then(function () {
            nelsAlertService.alert.isModal = false;
        });
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

        self.sortDataSetTypes();
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

            self.sortDataSetTypes();
        }
    };

    self.firstPage = function () {
        if (paginationService.pages[0] != 1) {

            paginationService.firstPage();
            self.sortDataSetTypes();
        }
    };

    self.getNextClass = function () {
        return paginationService.getNextClass();
    };

    self.nextPage = function () {
        if (paginationService.pages[paginationService.pages.length - 1] != paginationService.totalPage) {
            paginationService.nextPage();

            self.sortDataSetTypes();
        }
    };

    self.lastPage = function () {
        if (paginationService.pages[paginationService.pages.length - 1] != paginationService.totalPage) {
            paginationService.lastPage();

            self.sortDataSetTypes();
        }
    };

    self.change = function (numberPerPage) {

        paginationService.change(numberPerPage);
        self.sortDataSetTypes();
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
        return "";
    };

    self.getSelectionClass = function () {
        return "hidden";
    };

    selectionService.clearSelection(self.selectedDatasetTypes);

    // export services
    $scope.nelsAlertService = nelsAlertService;
    $scope.selectionService = selectionService;
});