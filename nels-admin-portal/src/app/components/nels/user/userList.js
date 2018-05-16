'use strict';

app.controller(
    'userListController',
    function ($scope, $location, $log, NeLSAppSettings,
              nelsAuthService, nelsNavigatorService, selectionService, nelsUtilsService, nelsAlertService,
              userPersistenceService,
              sbiUserPersistenceService, paginationService) {
        var self = this;
        nelsNavigatorService.requireLogin();

        self.selectedUsers = [];
        self.searchText = '';

        self.sbiUsers = [];

        self.sortColumn = 'id';
        self.reverse = true;

        this.$onInit = function () {
            paginationService.init();
            paginationService.setNumberOfItemsPerPage(NeLSAppSettings.numberOfItemsPerPage);
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
                        self.users = results.data;
                        paginationService.totalSize = results.count;
                        paginationService.totalPage = Math.floor((paginationService.totalSize + paginationService.numberOfItemsPerPage - 1) / paginationService.numberOfItemsPerPage);

                        self.getSbiUserInfo();
                    });
            $log.info("users init finished.")
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

        self.search = function () {
            selectionService.clearSelection(self.selectedUsers);
            paginationService.init();

            self.searchUsers();
        };

        self.add = function () {
            //TODO create new user profile
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
                        self.users = results.data;
                        paginationService.totalSize = results.count;
                        paginationService.totalPage = Math.floor((paginationService.totalSize + paginationService.numberOfItemsPerPage - 1) / paginationService.numberOfItemsPerPage);

                        self.getSbiUserInfo();
                    });
        };

        self.sortUsers = function () {
            selectionService.clearSelection(self.selectedUsers);

            if (self.searchText) {
                self.searchUsers();
            } else {
                var sort;
                if (self.reverse) {
                    sort = "-" + self.sortColumn;
                } else {
                    sort = self.sortColumn;
                }
                userPersistenceService.loadUsers(paginationService.offset, paginationService.getSizePerPage(), sort).then(function (results) {
                    self.users = results.data;
                    paginationService.totalSize = results.count;
                    paginationService.totalPage = Math.floor((paginationService.totalSize + paginationService.numberOfItemsPerPage - 1) / paginationService.numberOfItemsPerPage);
                    self.getSbiUserInfo();
                });
            }

        };

        self.createSbiProfile = function (nelsUser) {
            sbiUserPersistenceService
                .createUser(nelsUser.name, nelsUser.email,
                    nelsUser.idpusername)
                .then(
                    function (result) {
                        if (result) {
                            self
                                .getUserByFederatedId(nelsUser.idpusername).hasSbiProfile = true;
                            nelsAlertService
                                .showSuccess("SBI Profile created successfully");
                        } else {
                            self
                                .getUserByFederatedId(nelsUser.idpusername).hasSbiProfile = false;
                            nelsAlertService
                                .showDanger("Failed to create SBI Profile");
                        }
                    },
                    function (response) {
                        nelsAlertService
                            .showDanger("Failed to create SBI Profile");
                    });
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
            userPersistenceService.searchUsers(self.searchText, paginationService.offset, sort)
                .then(function (results) {
                    self.users = results.data;
                    paginationService.totalSize = results.count;
                    paginationService.totalPage = Math.floor((paginationService.totalSize + paginationService.numberOfItemsPerPage - 1) / paginationService.numberOfItemsPerPage);
                    self.getSbiUserInfo();
                });
        };

        /*
        self.selectAll = function() {
            selectionService.addAll(self.users);
        };

        self.deleteSelected = function () {
            selectionService.clearSelection(self.selectedUsers);
        };

        self.toggleItemSelection = function (item) {
            selectionService.toggleItemSelection(item);
        };
        
        self.selectionCss = function (item) {
            return selectionService.selectionCss(item);
        };

        */

        self.getTotalSize = function () {
            return paginationService.totalSize;
        };

        /* paging */
        self.getPages = function () {
            return paginationService.getPages();
        };

        self.setCurrentPage = function (page) {
            selectionService.clearSelection(self.selectedUsers);
            paginationService.setCurrentPage(page);
            if (self.searchText) {
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
                if (self.searchText) {
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
                if (self.searchText) {
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
                if (self.searchText) {
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
                if (self.searchText) {
                    self.searchUsers();
                } else {
                    self.loadUsers();
                }
            }
        };

        self.change = function (numberPerPage) {
            selectionService.clearSelection(self.selectedUsers);
            paginationService.change(numberPerPage);
            if (self.searchText) {
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


        self.getSearchClass = function () {
            return "";
        };

        self.getAddClass = function () {
            return "invisible";
        };

        self.getSelectionClass = function () {
            return "invisible";
        };

        selectionService.clearSelection(self.selectedUsers);

        // export services
        $scope.nelsAlertService = nelsAlertService;
        $scope.selectionService = selectionService;
    });
