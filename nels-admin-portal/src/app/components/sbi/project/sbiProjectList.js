'use strict';
app.controller('sbiProjectListController', function ($scope, $log, sbiProjectPersistenceService, sbiProjectNavigatorService,
                                                     enumService, selectionService, nelsConfirmActionService, nelsNavigatorService, nelsAlertService,
                                                     nelsUtilsService, paginationService, NeLSAppSettings) {
    nelsNavigatorService.requireLogin();
    var self = this;
    self.sortColumn = 'utilization';
    self.reverse = true;
    self.limitToDescription = 150;
    self.selectedProjects = [];
    self.searchText = '';

    this.$onInit = function () {
        paginationService.init();
        paginationService.setNumberOfItemsPerPage(NeLSAppSettings.numberOfItemsPerPage);
        self.loadSbiProjects();
    };


    self.datasets = [];
    self.subtypes = [];

    self.loadSbiProjects = function () {
        var sort;
        if (self.reverse) {
            sort = "-" + self.sortColumn;
        } else {
            sort = self.sortColumn;
        }
        sbiProjectPersistenceService
            .loadSbiProjects(paginationService.offset, sort)
            .then(
                function (results) {
                    self.projects = results.data;
                    paginationService.totalSize = results.count;
                    paginationService.totalPage = Math.floor((paginationService.totalSize + paginationService.numberOfItemsPerPage - 1) / paginationService.numberOfItemsPerPage);
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

        if (self.searchText) {
            self.searchProjects();
        } else {
            var sort;
            if (self.reverse) {
                sort = "-" + self.sortColumn;
            } else {
                sort = self.sortColumn;
            }
            sbiProjectPersistenceService
                .loadSbiProjects(paginationService.offset, sort)
                .then(
                    function (results) {
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
        sbiProjectPersistenceService.searchProjects(self.searchText, paginationService.offset, sort).then(function (results) {
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

    self.projectChanged = function () {
        sbiProjectPersistenceService.loadSbiDatasets(
            self.selectedProject).then(
            function (datasets) {
                self.datasets = datasets;
                self.subtypes = [];
            });
    };

    self.DatasetChanged = function () {
        self.subtypes = [];
        sbiProjectPersistenceService
            .loadSbiSubtypes(self.selectedProject,
                self.selectedDataset)
            .then(
                function (data) {
                    for (var i = 0; i < data.subtypes.length; i++) {
                        sbiProjectPersistenceService
                            .getNeLSLink(
                                self.selectedProject,
                                self.selectedDataset,
                                data.subtypes[i])
                            .then(
                                function (response) {
                                    self.subtypes
                                        .push({
                                            url: response.url,
                                            type: response.type,
                                            size: response.size
                                        });
                                },
                                function (err) {
                                    nelsAlertService
                                        .showDanger(err);
                                });

                    }
                });
    };
    self.openInNeLS = function (subtype) {
        sbiProjectPersistenceService.getNeLSLink(
            self.selectedProject, self.selectedDataset,
            subtype).then(function (response) {
            nelsAlertService.showSuccess(response);
        }, function (err) {
            nelsAlertService.showDanger(err);
        });
    };

    self.getMetaData = function (subtype) {
        return sbiProjectPersistenceService.getMetaData(self.selectedProject, self.selectedDataset, subtype.type).then(function (data) {
            /*
            var a = document.createElement("a");
            document.body.appendChild(a);
            a.style = "display: none";
            var blob = new Blob([data], {type: "application/*"});
            var url = window.URL.createObjectURL(blob);
            a.href = url;
            a.download = "metadata" + Math.random() + ".xlsx";
            a.click();
            window.URL.revokeObjectURL(url);
        */

        }, function (err) {
            return false;
        });
    };

    self.getProjectDetail = function (project) {
        sbiProjectNavigatorService.viewProjectDetail("app.sbiProjects.detail", project.id);
    };


    self.deleteSelected = function () {

        var modalOptions = {
            headerText: 'Confirm Deletion ',
            bodyText: (self.selectedProjects.length === 1) ? self.selectedProjects[0].name
                : self.selectedProjects.length + ' selected projects',
            actionButtonText: 'Delete',
            closeButtonText: 'Cancel'
        };

        var modalInstance = nelsConfirmActionService
            .showConfirmDialog(modalOptions);
        modalInstance.result.then(function (isConfirmed) {
            //do stuff on confirmation
            var count = self.selectedProjects.length;

            for (var i = 0; i < self.selectedProjects.length; i++) {
                var project = self.selectedProjects[i];
                var name = project.name;
                sbiProjectPersistenceService.deleteSbiProject(project.id,
                    function (successResponse) {
                        count--;
                        nelsAlertService.showSuccess(name + " is deleted.");
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


    self.toggleItemSelection = function (item) {
        selectionService.toggleItemSelection(item);
    };

    self.selectAll = function () {
        selectionService.addAll(self.projects);
    };

    self.selectionCss = function (item) {
        return selectionService.selectionCss(item);
    };


    self.getPercentage = function (sbiProject) {
        return Math.ceil(sbiProject.utilization * 100);
    };

    self.getPercentageCss = function (sbiProject) {
        var percentage = self.getPercentage(sbiProject);
        return (percentage >= 100 ? "danger" : (percentage >= 80 ? "warning" : "info"));
    };

    self.getDescription = function (projectDescription) {
        if (projectDescription.length <= self.limitToDescription) {
            return projectDescription;
        } else {
            return projectDescription.substring(0, self.limitToDescription) + "...";
        }
    };


    self.add = function () {
        nelsAlertService.showInfo("Projects can be created from the Quota page.");
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
        self.sortProjects();
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
            self.sortProjects();
        }
    };

    self.firstPage = function () {
        if (paginationService.pages[0] != 1) {
            selectionService.clearSelection(self.selectedProjects);
            paginationService.firstPage();
            self.sortProjects();
        }
    };


    self.getNextClass = function () {
        return paginationService.getNextClass();
    };

    self.nextPage = function () {
        if (paginationService.pages[paginationService.pages.length - 1] != paginationService.totalPage) {
            paginationService.nextPage();
            selectionService.clearSelection(self.selectedProjects);
            self.sortProjects();
        }
    };

    self.lastPage = function () {
        if (paginationService.pages[paginationService.pages.length - 1] != paginationService.totalPage) {
            paginationService.lastPage();
            selectionService.clearSelection(self.selectedProjects);
            self.sortProjects();
        }
    };

    self.change = function (numberPerPage) {
        selectionService.clearSelection(self.selectedProjects);
        paginationService.change(numberPerPage);
        self.sortProjects();
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
        return "hidden";
    };

    self.getSelectionClass = function () {
        return "";
    };

    selectionService.clearSelection(self.selectedProjects);


    // export services
    $scope.nelsAlertService = nelsAlertService;
    $scope.selectionService = selectionService;
});