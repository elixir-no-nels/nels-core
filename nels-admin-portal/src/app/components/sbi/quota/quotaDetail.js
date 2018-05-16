'use strict'
app.controller('quotaDetailController', function ($scope, $log, paginationService, nelsConfirmActionService, selectionService, sbiProjectPersistenceService, nelsAlertService, NeLSAppSettings, $stateParams, $uibModal, nelsNavigatorService, quotaPersistenceService, quotaNavigatorService, nelsProjectNavigatorService, sbiProjectNavigatorService) {
    nelsNavigatorService.requireLogin();
    var self = this;

    self.id = $stateParams.quotaId;
    self.quota = {};
    self.sortColumn = 'name';
    self.reverse = true;
    self.limitToDescription = 150;
    self.projects = [];
    self.selectedProjects = [];

    this.$onInit = function () {
        selectionService.clearSelection(self.selectedProjects);
        paginationService.init();
        paginationService.setNumberOfItemsPerPage(NeLSAppSettings.numberOfItemsPerPage);
        var sort;
        if (self.reverse) {
            sort = "-" + self.sortColumn;
        } else {
            sort = self.sortColumn;
        }
        quotaPersistenceService.loadProjectsInQuota(self.id, 0, sort, function (successResponse) {
            self.projects = successResponse.data.data;
            paginationService.totalSize = successResponse.data.count;
            paginationService.totalPage = Math.floor((paginationService.totalSize + paginationService.numberOfItemsPerPage - 1) / paginationService.numberOfItemsPerPage);
        }, function (errorResponse) {

        });
        self.getQuotaDetail();
    };

    self.loadSbiProjects = function () {
        var sort;
        if (self.reverse) {
            sort = "-" + self.sortColumn;
        } else {
            sort = self.sortColumn;
        }
        quotaPersistenceService.loadProjectsInQuota(self.id, paginationService.offset, sort, function (successResponse) {
            self.projects = successResponse.data.data;
            paginationService.totalSize = successResponse.data.count;
            paginationService.totalPage = Math.floor((paginationService.totalSize + paginationService.numberOfItemsPerPage - 1) / paginationService.numberOfItemsPerPage);
        }, function (errorResponse) {

        });
    };

    self.getQuotaDetail = function () {
        quotaPersistenceService.loadQuota(self.id, function (successResponse) {
            $log.info(successResponse);
            self.quota = successResponse.data;
        }, function (errorResponse) {

        });
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
        var sort;
        if (self.reverse) {
            sort = "-" + self.sortColumn;
        } else {
            sort = self.sortColumn;
        }
        quotaPersistenceService.loadProjectsInQuota(self.id, paginationService.offset, sort, function (successResponse) {
            self.projects = successResponse.data.data;
            paginationService.totalSize = successResponse.data.count;
            paginationService.totalPage = Math.floor((paginationService.totalSize + paginationService.numberOfItemsPerPage - 1) / paginationService.numberOfItemsPerPage);
        }, function (errorResponse) {

        });
    };

    self.deleteSelected = function () {
        var modalOptions = {
            headerText: 'Confirm Deletion',
            bodyText: 'Are you sure that you want to delete projects of this quota?',
            actionButtonText: 'Delete',
            closeButtonText: 'Cancel'
        };

        var modalInstance = nelsConfirmActionService.showConfirmDialog(modalOptions);
        modalInstance.result.then(function (isConfirmed) {
            var selectedProjects = selectionService.getItems();

            var count = selectedProjects.length;

            for (var i = 0; i < selectedProjects.length; i++) {
                var project = selectedProjects[i];
                var name = project.name;
                sbiProjectPersistenceService.deleteSbiProject(project.id,
                    function (successResponse) {
                        count--;
                        nelsAlertService.showInfo(name + " is deleted.");
                        if (count == 0) {
                            selectionService.clearSelection(self.selectedProjects);
                            self.sortProjects();
                        }
                    },
                    function (errorResponse) {
                        count--;

                        if (errorResponse.status == 412) {
                            nelsAlertService.showWarning(name + " can't be deleted. " + errorResponse.data.description);
                        } else {
                            nelsAlertService.showWarning(name + " is not deleted.");
                        }
                        if (count == 0) {
                            selectionService.clearSelection(self.selectedProjects);
                            self.sortProjects();
                        }
                    });
            }
        });
    };

    self.change = function (numberPerPage) {
        selectionService.clearSelection(self.selectedProjects);
        paginationService.change(numberPerPage);
        self.loadSbiProjects();
    };

    self.toggleItemSelection = function (item) {
        selectionService.toggleItemSelection(item);
    };

    self.selectAll = function () {
        selectionService.addAll(self.projects);
    };

    self.selectionCss = function (item) {
        return selectionService.selectionCss(item);
    };

    self.getSizeOfSelection = function () {
        return self.projects.length;
    };

    self.clearSelection = function () {
        selectionService.removeAll();
    };

    self.getDescription = function (projectDescription) {
        if (projectDescription.length <= self.limitToDescription) {
            return projectDescription;
        } else {
            return projectDescription.substring(0, self.limitToDescription) + "...";
        }
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

        self.loadSbiProjects();
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
            self.loadSbiProjects();
        }
    };

    self.firstPage = function () {
        if (paginationService.pages[0] != 1) {
            selectionService.clearSelection(self.selectedProjects);
            paginationService.firstPage();
            self.loadSbiProjects();
        }
    };


    self.getNextClass = function () {
        return paginationService.getNextClass();
    };

    self.nextPage = function () {
        if (paginationService.pages[paginationService.pages.length - 1] != paginationService.totalPage) {
            paginationService.nextPage();
            selectionService.clearSelection(self.selectedProjects);
            self.loadSbiProjects();
        }
    };

    self.lastPage = function () {
        if (paginationService.pages[paginationService.pages.length - 1] != paginationService.totalPage) {
            paginationService.lastPage();
            selectionService.clearSelection(self.selectedProjects);
            self.loadSbiProjects();
        }
    };

    self.startNumber = function () {
        return paginationService.startNumber();
    };

    self.endNumber = function () {
        return paginationService.endNumber();
    };

    self.editQuota = function () {
        var modal = $uibModal.open({
            animation: true,
            templateUrl: 'app/components/sbi/quota/editQuotaModal.html',
            controller: 'editQuotaModalController',
            controllerAs: 'eqctrl',
            size: 'md',
            resolve: {
                quota: function () {
                    return self.quota;
                }
            }
        });

        modal.result.then(function (response) {
            quotaPersistenceService.updateQuota(self.quota.id, response, function (successResponse) {
                nelsAlertService.showInfo("The quota is updated.");
                self.getQuotaDetail();
            }, function (errorResponse) {
                if (errorResponse.status == 409) {
                    nelsAlertService.showWarning(errorResponse.data);
                }
            });
        });

        modal.closed.then(function () {
        });
    };

    self.deleteQuota = function () {

        $log.info("delete quota called");

        var modalOptions = {
            headerText: 'Confirm Deletion',
            bodyText: 'Are you sure that you want to delete quota ' + self.quota.name + '?',
            actionButtonText: 'Delete',
            closeButtonText: 'Cancel'
        };

        var modalInstance = nelsConfirmActionService.showConfirmDialog(modalOptions);
        modalInstance.result.then(function (isConfirmed) {
            //do stuff on confirmation

            var name = self.quota.name;
            quotaPersistenceService.deleteQuota(self.quota.id, function (successResponse) {
                nelsAlertService.showSuccess(name + " is deleted.");
                quotaNavigatorService.viewQuotas();
            }, function (errorResponse) {
                if (errorResponse.status == 412) {
                    nelsAlertService.showWarning(name + " can't be deleted. " + errorResponse.data.description);
                } else {
                    nelsAlertService.showDanger(name + " is not deleted.");
                }
            });

        });
    };

    self.add = function () {
        var quotaId = self.quota.quota_id;

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
                $log.info(successResponse.data.id);
                nelsAlertService.showInfo("The project is created.");
                self.loadSbiProjects();
            }, function (errorResponse) {
                if (errorResponse.status == 412 || errorResponse.status == 409) {
                    nelsAlertService.showWarning(errorResponse.data);
                }
            });
        });
        modal.closed.then(function () {

        });
    };

    // export services
    $scope.nelsAlertService = nelsAlertService;
    $scope.nelsProjectNavigatorService = nelsProjectNavigatorService;
    $scope.sbiProjectNavigatorService = sbiProjectNavigatorService;
    $scope.selectionService = selectionService;
});