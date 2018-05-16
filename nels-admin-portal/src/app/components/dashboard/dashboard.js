'use strict';
/* login controller */
app.controller('dashboardController', function ($scope, $location, $log, $uibModal,
                                                NeLSAppSettings,
                                                nelsAuthService,
                                                nelsNavigatorService,
                                                userPersistenceService,
                                                nelsProjectPersistenceService,
                                                nelsAlertService,
                                                nelsConfirmActionService,
                                                quotaPersistenceService,
                                                sbiProjectPersistenceService,
                                                userNavigatorService,
                                                quotaNavigatorService,
                                                nelsProjectNavigatorService,
                                                sbiProjectNavigatorService,
                                                dashboardPersistenceService) {
    nelsNavigatorService.requireLogin();
    var self = this;

    self.user_count = 0;
    self.userType = "";
    self.nels_project_count = 0;
    self.sbi_quota_count = 0;
    self.sbi_project_count = 0;
    self.sbi_quota_total_allocated = 0;
    self.sbi_quota_total_used = 0;

    self.top_nels_users = [];
    self.top_nels_projects = [];
    self.top_used_quotas = [];
    self.top_used_sbi_projects = [];
    self.disk_usage_last_update = 0;

    self.nels_disk_total = 0;
    self.nels_used_personal = 0;
    self.nels_used_project = 0;
    self.nels_disk_usage_last_update = 0;

    self.quotaAllocationPercentage = function () {
        return quotaPersistenceService.blockQuota.value > 0 ? Math.floor(100 * self.sbi_quota_total_allocated / quotaPersistenceService.blockQuota.value) : 0;
    }

    self.quotaUsagePercentage = function () {
        return quotaPersistenceService.blockQuota.value > 0 ? Math.floor(100 * self.sbi_quota_total_used / quotaPersistenceService.blockQuota.value) : 0;
    }

    self.percentageCss = function (percentage) {
        return percentage > 100 ? "danger" : (percentage >= 80 ? "warning" : "default");
    }


    $scope.quotaChart = [[["Used Quota", 0], ["Unused Quota", 0]]];
    $scope.quotaChartOptions = {
        grid: {
            drawBorder: false,
            drawGridlines: false,
            background: '#ffffff',
            shadow: false
        },
        seriesDefaults: {
            // Make this a pie chart.
            renderer: jQuery.jqplot.PieRenderer,
            rendererOptions: {
                // Put data labels on the pie slices.
                // By default, labels show the percentage of the slice.
                //showDataLabels: true
                //sliceMargin: 5,
                // Pies and donuts can start at any arbitrary angle.
                startAngle: -90,
                showDataLabels: true
                // By default, data labels show the percentage of the donut/pie.
                // You can show the data 'value' or data 'label' instead.
                //dataLabels: 'value',
                // "totalLabel=true" uses the centre of the donut for the total amount
            }
        },
        legend: {show: true, location: 'e'}

    };

    self.nelsChart = [[["Personal Area", 0], ["Project Area", 0], ["Unused", 0]]];
    self.nelsChartOptions = {
        grid: {
            drawBorder: false,
            drawGridlines: false,
            background: '#ffffff',
            shadow: false
        },
        seriesDefaults: {
            // Make this a pie chart.
            renderer: jQuery.jqplot.DonutRenderer,
            rendererOptions: {
                // Put data labels on the pie slices.
                // By default, labels show the percentage of the slice.
                //showDataLabels: true
                //sliceMargin: 5,
                // Pies and donuts can start at any arbitrary angle.
                startAngle: -90,
                showDataLabels: true
                // By default, data labels show the percentage of the donut/pie.
                // You can show the data 'value' or data 'label' instead.
                //dataLabels: 'value',
                // "totalLabel=true" uses the centre of the donut for the total amount
            }
        },
        legend: {show: true, location: 'e'}

    };

    self.canEditBlockQuota = function () {
        return self.userType === 'Administrator';
    };


    self.init = function () {

        userPersistenceService.countUsers().then(function (result) {
            self.user_count = result;
        });

        nelsProjectPersistenceService.countProjects().then(function (result) {
            self.nels_project_count = result;
        });

        quotaPersistenceService.countQuotas().then(function (result) {
            self.sbi_quota_count = result;
        });

        quotaPersistenceService.quotaSizes().then(function (result) {
            self.sbi_quota_total_allocated = result.allocated;
            self.sbi_quota_total_used = result.used;


            // $log.info("total allocated:" + self.sbi_quota_total_allocated + ",total used:" + self.sbi_quota_total_used +
            // ",block quota old value:" + self.blockQuota.old_value + ", block quota new value:" + self.blockQuota.new_value);

            $scope.quotaChart = [[
                ["Allocated and used", self.sbi_quota_total_used],
                ["Allocated but not used", self.sbi_quota_total_allocated - self.sbi_quota_total_used],
            ]];
        });

        sbiProjectPersistenceService.countSbiProjects().then(function (result) {
            self.sbi_project_count = result;
        });


        userPersistenceService
            .loadUsers(0, NeLSAppSettings.numberOfItemsPerPage, "-disk_usage")
            .then(
                function (results) {
                    self.top_nels_users = results.data;
                });

        nelsProjectPersistenceService
            .loadProjects(0, "-disk_usage")
            .then(
                function (results) {
                    self.top_nels_projects = results.data;
                });


        quotaPersistenceService.loadSbiQuotas(0, NeLSAppSettings.numberOfItemsPerPage, "-used_size").then(function (result) {
            self.top_used_quotas = result.data;

        });

        sbiProjectPersistenceService.loadSbiProjects(0, "-disk_usage").then(function (result) {
            self.top_used_sbi_projects = result.data;
            self.disk_usage_last_update = result.data[0].disk_usage_last_update;
        });

        quotaPersistenceService.loadBlockQuota();
        dashboardPersistenceService.loadDashboardInfo().then(function (response) {
            self.nels_used_personal = response.data["nels_disk_personal_all"];
            self.nels_used_project = response.data["nels_disk_projects_all"];
            self.nels_disk_total = response.data["nels_disk_total"];
            var nels_unused = self.nels_disk_total - self.nels_used_personal - self.nels_used_project;
            self.nelsChart = [[["Personal Area", self.nels_used_personal], ["Project Area", self.nels_used_project], ["Unused", nels_unused]]];
            self.nels_disk_usage_last_update = response.data["nels_disk_usage_last_update"];
            //self.disk_usage_last_update = response.data["disk_usage_last_update"];
        });
    };

    self.init();

    self.viewUsers = function () {
        userNavigatorService.viewUsers();
    };
    self.viewNelsProjects = function () {
        nelsProjectNavigatorService.viewProjects();
    };

    self.viewSbiQuotas = function () {
        quotaNavigatorService.viewQuotas();
    };

    self.viewSbiProjects = function () {
        sbiProjectNavigatorService.viewProjects();
    };

    self.userType = nelsAuthService.auth.userType;
    //export services
    $scope.nelsAlertService = nelsAlertService;
    $scope.userNavigatorService = userNavigatorService;
    $scope.quotaNavigatorService = quotaNavigatorService;
    $scope.quotaPersistenceService = quotaPersistenceService;
    $scope.nelsProjectNavigatorService = nelsProjectNavigatorService;
    $scope.sbiProjectNavigatorService = sbiProjectNavigatorService;

});