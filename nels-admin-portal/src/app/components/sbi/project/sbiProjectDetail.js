'use strict';
app.controller(
    'sbiProjectDetailController',
    function ($scope, $log, $stateParams, $uibModal, nelsUtilsService,
              nelsConfirmActionService, sbiProjectPersistenceService, selectionService,
              nelsNavigatorService,
              nelsAlertService,
              quotaNavigatorService) {
        nelsNavigatorService.requireLogin();
        var self = this;
        self.project_id = $stateParams.projectId;

        self.project = {};
        self.members = [];
        self.selectedMembers = [];
        self.sortColumn = 'name';
        self.reverse = true;

        self.getProjectDetail = function () {
            sbiProjectPersistenceService.loadSbiProject(self.project_id).then(function (results) {
                self.project = results;
            });
            self.getMembers();
        };

        self.getMembers = function () {
            sbiProjectPersistenceService.loadSbiProjectMembers(self.project_id).then(function (results) {
                self.members = results;
            });
        };

        self.sortBy = function (sortColumn) {
            $log.info("sorting project member");
            self.reverse = (self.sortColumn === sortColumn) ? !self.reverse
                : false;
            self.sortColumn = sortColumn;
        };

        self.getSizeOfSelection = function () {
            return self.selectedMembers.length;
        };

        self.clearSelection = function () {
            selectionService.clearSelection(self.selectedMembers);
        };

        self.selectAll = function () {
            $log.info("select all members");
            selectionService.addAll(self.members);
        };

        self.deleteSelected = function () {

            var items = self.selectedMembers;


            var modalOptions = {
                headerText: 'Confirm Removal ',
                bodyText: (items.length === 1) ? items[0].name
                    : items.length + ' members selected',
                actionButtonText: 'Remove',
                closeButtonText: 'Cancel'
            };

            var modalInstance = nelsConfirmActionService
                .showConfirmDialog(modalOptions);
            modalInstance.result.then(function (isConfirmed) {
                //do stuff on confirmation
                var deletedMembers = [];
                items.forEach(function (item) {
                    deletedMembers.push(item.federated_id);
                });
                sbiProjectPersistenceService.modifyMembers(self.project_id, {
                    "method": "delete",
                    "data": deletedMembers
                }, function (successResponse) {
                    selectionService.clearSelection(self.selectedMembers);
                    self.getMembers();
                }, function (errorResponse) {

                });

            });


        };

        self.toggleItemSelection = function (item) {
            selectionService.toggleItem(self.selectedMembers, item);
        };

        self.selectionCss = function (item) {
            return selectionService.selectionCss(self.selectedMembers, item);
        };

        self.editProject = function () {
            var modal = $uibModal.open({
                animation: true,
                templateUrl: 'app/components/sbi/project/editSbiProjectModal.html',
                controller: 'editSbiProjectModalController',
                controllerAs: 'espctrl',
                size: 'lg',
                resolve: {
                    project: function () {
                        return self.project
                    }
                }
            });

            modal.result.then(function (response) {
                var data = response;
                sbiProjectPersistenceService.updateSbiProject(self.project.id, data, function (successResponse) {
                    nelsAlertService.showInfo("The project is updated.");
                    self.getProjectDetail();
                }, function (errorResponse) {
                    if (errorResponse.status == 409) {
                        nelsAlertService.showWarning(errorResponse.data);
                    }
                });
            });
            modal.closed.then(function () {

            });
        };

        self.addUser = function () {
            self.add(10);
        };

        self.addAdmin = function () {
            self.add(1);
        };
        self.add = function (role) {

            var modal = $uibModal.open({
                animation: true,
                templateUrl: 'app/components/sbi/project/addUserModal.html',
                controller: 'addUserModalController',
                controllerAs: 'umctrl',
                size: 'lg',
                scope: $scope
            });
            modal.result.then(function (response) {
                var selectedUsers = response;

                var membersWithoutSbi = [];
                var addedMembers = [];
                for (var i = 0; i < selectedUsers.length; i++) {
                    var selectedUser = selectedUsers[i];

                    if (selectedUser.hasSbiProfile) {

                        var added = true;
                        for (var j = 0; j < self.members.length; j++) {
                            var member = self.members[j];
                            if (selectedUser.idpusername == member.federated_id) {
                                added = false;
                            }
                        }
                        if (added) {
                            addedMembers.push({"federated_id": selectedUser.idpusername, "role": role})
                        }
                    } else {
                        membersWithoutSbi.push(selectedUser.name);
                    }
                }

                if (membersWithoutSbi.length != 0) {
                    var names = '';
                    membersWithoutSbi.forEach(function (name) {
                        names = names + name + ', '
                    });
                    nelsAlertService.showWarning("You can't add those users " + names + "because they don't have sbi profile.");
                }
                if (addedMembers.length != 0) {
                    sbiProjectPersistenceService.modifyMembers(self.project_id, {
                        "method": "add",
                        "data": addedMembers
                    }, function (successResponse) {
                        selectionService.clearSelection(self.selectedMembers);
                        self.getMembers();
                    }, function (errorResponse) {

                    });
                }
            });
            modal.closed.then(function () {
                selectionService.clearSelection(self.selectedMembers);
            });
        };

        selectionService.clearSelection(self.selectedMembers);
        self.getProjectDetail();

        // export services
        $scope.nelsAlertService = nelsAlertService;
        $scope.selectionService = selectionService;
    });