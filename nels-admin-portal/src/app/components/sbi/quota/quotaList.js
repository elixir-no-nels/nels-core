'use strict';
app.controller('quotaListController', function ($scope, $location, $log, $uibModal,
                                                NeLSAppSettings, nelsAuthService, nelsNavigatorService, sbiProjectNavigatorService, sbiProjectPersistenceService, quotaNavigatorService,
                                                selectionService, quotaPersistenceService, nelsAlertService, nelsUtilsService, nelsConfirmActionService, paginationService) {
    var self = this;
    nelsNavigatorService.requireLogin();


    self.sortColumn = 'utilization';
    self.reverse = true;
    self.total_num_projects = 0;
    self.total_quota;
    self.total_used_quota;
    self.searchText = '';
    self.selectedQuotas = [];

    this.$onInit = function () {
        paginationService.init();
        paginationService.setNumberOfItemsPerPage(NeLSAppSettings.numberOfItemsPerPage);

        self.loadSbiQuotas();
    };

    self.addCreator = function () {
        self.quotas.forEach(function (quota) {
            quota.creator = quota.firstname + ' ' + quota.surname;
        });
    };

    self.computeTotal = function () {
        var total_quota = 0;
        var total_used_quota = 0;
        var total_num_projects = 0;
        self.quotas.forEach(function (quota) {
            total_num_projects = total_num_projects + quota.num_of_projects;
            total_quota = total_quota + quota.quota_size;
            total_used_quota = total_used_quota + quota.used_size;
        });
        self.total_num_projects = total_num_projects;
        self.total_quota = total_quota;
        self.total_used_quota = total_used_quota;
    };

    self.finish = function () {
        self.addCreator();
        self.computeTotal();
    };

    self.loadSbiQuotas = function () {
        var sort;
        if (self.reverse) {
            sort = "-" + self.sortColumn;
        } else {
            sort = self.sortColumn;
        }
        quotaPersistenceService.loadSbiQuotas(paginationService.offset, paginationService.getSizePerPage(), sort).then(function (result) {
            self.quotas = result.data;
            paginationService.totalSize = result.count;
            paginationService.totalPage = Math.floor((paginationService.totalSize + paginationService.numberOfItemsPerPage - 1) / paginationService.numberOfItemsPerPage);

            if (!paginationService.pages) {
                if (paginationService.totalPage > paginationService.defaultLengthForPages) {
                    paginationService.pages = nelsUtilsService.range(1, paginationService.defaultLengthForPages);
                } else {
                    paginationService.pages = nelsUtilsService.range(1, paginationService.totalPage);
                }
            }
            self.finish();
        });
    };


    self.sortBy = function (sortColumn) {
        paginationService.init();
        self.reverse = (self.sortColumn === sortColumn) ? !self.reverse
            : false;
        self.sortColumn = sortColumn;
        self.sortQuotas();

    };

    self.sortQuotas = function () {
        selectionService.clearSelection(self.selectedQuotas);

        if (self.searchText) {
            self.searchQuotas();
        } else {
            var sort;
            if (self.reverse) {
                sort = "-" + self.sortColumn;
            } else {
                sort = self.sortColumn;
            }
            quotaPersistenceService.loadSbiQuotas(paginationService.offset, paginationService.getSizePerPage(), sort).then(function (results) {
                self.quotas = results.data;
                paginationService.totalSize = results.count;
                paginationService.totalPage = Math.floor((paginationService.totalSize + paginationService.numberOfItemsPerPage - 1) / paginationService.numberOfItemsPerPage);
                if (paginationService.totalPage > paginationService.defaultLengthForPages) {
                    paginationService.pages = nelsUtilsService.range(1, paginationService.defaultLengthForPages);
                } else {
                    paginationService.pages = nelsUtilsService.range(1, paginationService.totalPage);
                }
                self.finish();
            });
        }


    };

    self.search = function () {
        selectionService.clearSelection(self.selectedQuotas);
        paginationService.init();

        self.searchQuotas();

    };

    self.add = function () {
        var modal = $uibModal.open({
            animation: true,
            templateUrl: 'app/components/sbi/quota/createQuotaModal.html',
            controller: 'createQuotaModalController',
            controllerAs: 'cqctrl',
            size: 'md',
            scope: $scope
        });

        modal.result.then(function (response) {
            quotaPersistenceService.createQuota(response, function (successResponse) {
                nelsAlertService.showSuccess("A quota is created.");
                self.sortQuotas();
            }, function (errorResponse) {
                if (errorResponse.status == 412 || errorResponse.status == 409) {
                    nelsAlertService.showWarning(errorResponse.data);
                }
                else {
                    nelsAlertService.showDanger(errorResponse.data.description);
                }
            });
        });

        modal.closed.then(function () {
        });
    };

    self.getQuotaDetail = function (quota) {
        quotaNavigatorService.viewQuotaDetail("app.sbiQuota.detail", quota.id);
    };

    self.searchQuotas = function () {

        var sort;
        if (self.reverse) {
            sort = "-" + self.sortColumn;
        } else {
            sort = self.sortColumn;
        }
        quotaPersistenceService.searchQuotas(self.searchText, paginationService.offset, sort)
            .then(function (results) {
                self.quotas = results.data;
                paginationService.totalSize = results.count;
                paginationService.totalPage = Math.floor((paginationService.totalSize + paginationService.numberOfItemsPerPage - 1) / paginationService.numberOfItemsPerPage);
                if (paginationService.totalPage > paginationService.defaultLengthForPages) {
                    paginationService.pages = nelsUtilsService.range(1, paginationService.defaultLengthForPages);
                } else {
                    paginationService.pages = nelsUtilsService.range(1, paginationService.totalPage);
                }
                self.finish();
            });
    };

    self.createSbiProject = function (selectedQuota) {
        var quotaId = selectedQuota.quota_id;

        var modal = $uibModal.open({
            animation: true,
            templateUrl: 'app/components/sbi/quota/createSbiProjectModal.html',
            controller: 'createSbiProjectModalController',
            controllerAs: 'cspctrl',
            size: 'lg',
            scope: $scope
        });

        modal.result.then(function (response) {
            var data = response;
            data["quota_id"] = quotaId;
            sbiProjectPersistenceService.createSbiProject(data, function (successResponse) {
                nelsAlertService.showSuccess("The project is created.");
                nelsNavigatorService.goToProjectDetailPage(successResponse.data.id);
            }, function (errorResponse) {
                if (errorResponse.status == 412 || errorResponse.status == 409) {
                    nelsAlertService.showWarning(errorResponse.data);
                }
            });
        });
        modal.closed.then(function () {

        });
    };


    self.toggleItemSelection = function (item) {
        selectionService.toggleItemSelection(item);
    };

    self.selectAll = function () {
        selectionService.addAll(self.quotas);
    };

    self.selectionCss = function (item) {
        return selectionService.selectionCss(item);
    };

    self.getPercentage = function (q) {
        return Math.ceil(q.utilization * 100);
    };

    self.getPercentageCss = function (q) {
        var percentage = self.getPercentage(q);
        return (percentage >= 100 ? "danger" : (percentage >= 80 ? "warning" : "info"));
    };

    self.getAddClass = function () {
        return "";
    };
    self.getSelectionClass = function () {
        return "hidden";
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
        selectionService.clearSelection(self.selectedQuotas);
        self.sortQuotas();
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
            selectionService.clearSelection(self.selectedQuotas);
            self.sortQuotas();
        }
    };

    self.firstPage = function () {
        if (paginationService.pages[0] != 1) {
            selectionService.clearSelection(self.selectedQuotas);
            paginationService.firstPage();
            self.sortQuotas();
        }
    };

    self.getNextClass = function () {
        return paginationService.getNextClass();
    };

    self.nextPage = function () {
        if (paginationService.pages[paginationService.pages.length - 1] != paginationService.totalPage) {
            paginationService.nextPage();
            selectionService.clearSelection(self.selectedQuotas);
            self.sortQuotas();
        }
    };

    self.lastPage = function () {
        if (paginationService.pages[paginationService.pages.length - 1] != paginationService.totalPage) {
            paginationService.lastPage();
            selectionService.clearSelection(self.selectedQuotas);
            self.sortQuotas();
        }
    };

    self.change = function (numberPerPage) {
        selectionService.clearSelection(self.selectedQuotas);
        paginationService.change(numberPerPage);
        self.sortQuotas();
    };

    self.startNumber = function () {
        return paginationService.startNumber();
    };

    self.endNumber = function () {
        return paginationService.endNumber();
    };

    self.getDeleteSelectionText = function () {
        return "Delete Quota(s)";
    };

    selectionService.clearSelection(self.selectedQuotas);

    // export services
    $scope.nelsAlertService = nelsAlertService;
});