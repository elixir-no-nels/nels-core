'use strict';
/* login controller */

app.controller('menuController', function ($scope,$state, $log, $uibModal, NeLSAppSettings, localStorageService,
                                           nelsAuthService,
                                           nelsNavigatorService,
                                           nelsAlertService,
                                           userNavigatorService,
                                           quotaNavigatorService,
                                           quotaPersistenceService,
                                           nelsProjectNavigatorService,
                                           sbiProjectNavigatorService,
                                           sbiDataSetTypeNavigatorService) {

    var self = this;
    self.message = "";
    self.name = "";

    self.logOut = function () {
        nelsAuthService.logOut();
        nelsNavigatorService.goHome();
    };

    self.isAuth = function () {
        var data = localStorageService.get(nelsAuthService.getAuthorizationDataKey());
        if (data) {
            self.name = data.name;
            return true;
        } else {
            return false;
        }
    };

    self.viewUsers = function () {
        _paq.push(['trackEvent', 'View', 'Users']);
        userNavigatorService.viewUsers();
    };

    self.viewNelsProjects = function () {
        _paq.push(['trackEvent', 'View', 'NeLS Projects']);
        nelsProjectNavigatorService.viewProjects();
    };

    self.viewSbiQuotas = function () {
        _paq.push(['trackEvent', 'View', 'Sbi Quotas']);
        quotaNavigatorService.viewQuotas();
    };

    self.viewSbiProjects = function () {
        _paq.push(['trackEvent', 'View', 'Sbi Projects']);
        sbiProjectNavigatorService.viewProjects();
    };

    self.viewSbiDataSetTypes = function () {
        _paq.push(['trackEvent', 'View', 'Sbi Dataset types']);
        sbiDataSetTypeNavigatorService.viewDataSetTypes();
    };

    self.viewDashboard = function () {
        nelsNavigatorService.goHome();
    };

    self.editBlockQuota = function () {
        var modal = $uibModal.open({
            animation: true,
            templateUrl: 'app/components/sbi/quota/editBlockQuotaModal.html',
            controller: 'editBlockQuotaModalController',
            controllerAs: 'ebqctrl',
            size: 'md',
            resolve: {
                blockQuota: function () {
                    return quotaPersistenceService.blockQuota;
                }
            }
        });

        modal.result.then(function (response) {
            quotaPersistenceService.updateBlockQuota(response, function (successResponse) {
                nelsAlertService.showSuccess("Block quota is updated.");

                quotaPersistenceService.loadBlockQuota();
            }, function (errorResponse) {
                if (errorResponse.status != 201) {
                    nelsAlertService.showWarning(errorResponse.data);
                }
            });
        });

        modal.closed.then(function () {
        });
    };

    self.activeStateCss = function (stateName) {
        return $state.current.name.replace("app.", "").startsWith(stateName) ? "active" : "";
    };

    // export services
    $scope.NeLSAppSettings = NeLSAppSettings;
    $scope.nelsAuthService = nelsAuthService;
});