'use strict';

app.controller('addUserModalController', function (
    $log, NeLSAppSettings, nelsUtilsService, selectionService, userPersistenceService,
    paginationService, $uibModalInstance, nelsNavigatorService, sbiUserPersistenceService) {

    nelsNavigatorService.requireLogin();

    var self = this;
    self.modalTitle = "Select users";

    self.sortColumn = 'id';
    self.reverse = true;
    self.users = [];
    self.selectedUsers = [];
    selectionService.clearSelection(self.selectedUsers);

    this.$onInit = function () {
        paginationService.init();
        paginationService.setNumberOfItemsPerPage(5);
        var sort;
        if (self.reverse) {
            sort = "-" + self.sortColumn;
        } else {
            sort = self.sortColumn;
        }
        userPersistenceService
            .loadUsers(0, paginationService.getSizePerPage(), sort)
            .then(
                function (results) {
                    paginationService.totalSize = results.count;
                    paginationService.totalPage = Math.floor((paginationService.totalSize + paginationService.numberOfItemsPerPage - 1) / paginationService.numberOfItemsPerPage);
                    self.users = results.data;
                    self.getSbiUserInfo();
                });
    };


    self.getUserByFederatedId = function (federatedId) {
        return self.users.filter(function (a) {
            return a.idpusername == federatedId;
        })[0];
    };

    self.getSbiUserInfo = function () {
        var federatedIds = self.users.map(function (user) {
            return user.idpusername;
        });

        sbiUserPersistenceService
            .searchUsers(federatedIds)
            .then(
                function (sbiUsers) {
                    self.sbiUsers = sbiUsers;
                    for (var i = 0; i < self.sbiUsers.length; i++) {
                        self.getUserByFederatedId(self.sbiUsers[i].federated_id).hasSbiProfile = true;
                    }
                });
    };

    self.onSearch = function () {
        selectionService.clearSelection(self.selectedUsers);
        paginationService.init();

        self.searchUsers();
    };

    self.loadUsers = function () {
        var sort;
        if (self.reverse) {
            sort = "-" + self.sortColumn;
        } else {
            sort = self.sortColumn;
        }
        userPersistenceService
            .loadUsers(paginationService.offset, paginationService.getSizePerPage(), sort)
            .then(
                function (results) {
                    paginationService.totalSize = results.count;
                    paginationService.totalPage = Math.floor((paginationService.totalSize + paginationService.numberOfItemsPerPage - 1) / paginationService.numberOfItemsPerPage);
                    self.users = results.data;
                    self.getSbiUserInfo();
                });
    };

    self.sortUsers = function () {
        selectionService.clearSelection(self.selectedUsers);

        if (self.query) {
            self.searchUsers();
        } else {
            var sort;
            if (self.reverse) {
                sort = "-" + self.sortColumn;
            } else {
                sort = self.sortColumn;
            }
            userPersistenceService.loadUsers(paginationService.offset, paginationService.getSizePerPage(), sort).then(function (results) {
                paginationService.totalSize = results.count;
                paginationService.totalPage = Math.floor((paginationService.totalSize + paginationService.numberOfItemsPerPage - 1) / paginationService.numberOfItemsPerPage);
                self.users = results.data;
                self.getSbiUserInfo();
            });
        }

    };

    self.sortBy = function (sortColumn) {
        paginationService.init();
        self.reverse = (self.sortColumn === sortColumn) ? !self.reverse
            : false;
        self.sortColumn = sortColumn;
        self.sortUsers();
    };

    self.searchUsers = function () {

        var sort;
        if (self.reverse) {
            sort = "-" + self.sortColumn;
        } else {
            sort = self.sortColumn;
        }
        userPersistenceService.searchUsers(self.query, paginationService.offset, sort)
            .then(function (results) {
                paginationService.totalSize = results.count;
                paginationService.totalPage = Math.floor((paginationService.totalSize + paginationService.numberOfItemsPerPage - 1) / paginationService.numberOfItemsPerPage);
                self.users = results.data;
                self.getSbiUserInfo();
            });
    };

    self.selectAll = function () {
        selectionService.selectAll(self.users, self.selectedUsers);
    };

    self.clearSelection = function () {
        selectionService.clearSelection(self.selectedUsers);
    };

    self.getSizeOfSelection = function () {
        return self.selectedUsers.length;
    };

    self.toggleItemSelection = function (item) {
        selectionService.toggleItem(self.selectedUsers, item);
    };

    self.selectionCss = function (item) {
        return selectionService.selectionCss(self.selectedUsers, item);
    };

    self.getTotalSize = function () {
        return paginationService.totalSize;
    };

    self.getPages = function () {
        return paginationService.getPages();
    };

    self.setCurrentPage = function (page) {
        selectionService.clearSelection(self.selectedUsers);
        paginationService.setCurrentPage(page);
        if (self.query) {
            self.searchUsers();
        } else {
            self.loadUsers();
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
            selectionService.clearSelection(self.selectedUsers);
            if (self.query) {
                self.searchUsers();
            } else {
                self.loadUsers();
            }
        }
    };

    self.firstPage = function () {
        if (paginationService.pages[0] != 1) {
            selectionService.clearSelection(self.selectedUsers);
            paginationService.firstPage();
            if (self.query) {
                self.searchUsers();
            } else {
                self.loadUsers();
            }
        }
    };

    self.getNextClass = function () {
        return paginationService.getNextClass();
    };

    self.nextPage = function () {
        if (paginationService.pages[paginationService.pages.length - 1] != paginationService.totalPage) {
            paginationService.nextPage();
            selectionService.clearSelection(self.selectedUsers);
            if (self.query) {
                self.searchUsers();
            } else {
                self.loadUsers();
            }
        }
    };

    self.lastPage = function () {
        if (paginationService.pages[paginationService.pages.length - 1] != paginationService.totalPage) {
            paginationService.lastPage();
            selectionService.clearSelection(self.selectedUsers);
            if (self.query) {
                self.searchUsers();
            } else {
                self.loadUsers();
            }
        }
    };

    self.change = function (numberPerPage) {
        selectionService.clearSelection(self.selectedUsers);
        paginationService.change(numberPerPage);
        if (self.query) {
            self.searchUsers();
        } else {
            self.loadUsers();
        }
    };

    self.startNumber = function () {
        return paginationService.startNumber();
    };

    self.endNumber = function () {
        return paginationService.endNumber();
    };

    self.cancelClick = function () {
        $uibModalInstance.dismiss('cancel');
    };

    self.okClick = function () {
        $uibModalInstance.close(self.selectedUsers);
    };

});