'use strict';

var app = angular.module('NeLSApp', ['LocalStorageModule',
    'angular-loading-bar', 'ui.bootstrap', 'ui.router', 'ui.select', 'ngSanitize','ui.chart']);

app.filter('customizeDate', function() {
    return function(input) {
        return new Date(input.substring(0, input.indexOf(" "))).toISOString();
    };
});

app.constant(
        'NeLSAppSettings',
        {
            name: 'NeLS Admin Portal',
            apiUrl: '_apiUrl_',
            oauthUrl: '_oauthUrl_',
            logoutUrl: '_logoutUrl_',
            defaultLengthForPages: 10,
            numberOfItemsPerPage : 10
        });
app.component('menu', {templateUrl: 'app/shared/menu/menu.html', controller: 'menuController'});
app.component('footer', {templateUrl: 'app/shared/footer/footer.html', controller: 'footerController'});
app.component('login', {templateUrl: 'app/shared/login/login.html', controller: 'loginController'});
app.component('welcome', {templateUrl: 'app/shared/welcome/welcome.html', controller: 'welcomeController'});
app.component('dashboard', {templateUrl: 'app/components/dashboard/dashboard.html', controller: 'dashboardController'});
app.component('users', {templateUrl: 'app/components/nels/user/userList.html', controller: 'userListController'});
app.component('sbiProjectDetail', {templateUrl: 'app/components/sbi/project/sbiProjectDetail.html', controller: 'sbiProjectDetailController'});
app.component('sbiProjects', {templateUrl: 'app/components/sbi/project/sbiProjectList.html', controller: 'sbiProjectListController'});
app.component('nelsProjects', {templateUrl: 'app/components/nels/project/nelsProjectList.html', controller: 'nelsProjectListController'});
app.component('sbiQuotas', {templateUrl: 'app/components/sbi/quota/quotaList.html', controller: 'quotaListController'});
app.component('sbiQuotaDetail', {templateUrl: 'app/components/sbi/quota/quotaDetail.html', controller: 'quotaDetailController'});
app.component('sbiDataSetTypes', {templateUrl: 'app/components/sbi/datasettype/sbiDataSetTypeList.html', controller: 'sbiDataSetTypeListController'});
app.component('pagination', {templateUrl: 'app/shared/pagination/pagination.html', controller: 'paginationController', bindings: {getPreviousClass: '&', previousPage: '&', getPages: '&', getPageClass: '&', setCurrentPage: '&', getNextClass: '&', nextPage: '&', startNumber: '&', endNumber: '&', getTotalSize: '&', change: '&', firstPage: '&', lastPage: '&'}});
app.component('invalid', {templateUrl: 'app/shared/invalid.html'});
app.component('notfound', {templateUrl: 'app/shared/not-found.html'});
app.component('denied', {templateUrl: 'app/shared/access-denied.html'});

app.config(['$stateProvider', '$urlRouterProvider',
    function ($stateProvider, $urlRouterProvider) {
        $urlRouterProvider.otherwise('/');

        var states = [
            {name: 'app', url: '/', views: {'content@': {component: 'welcome'}}},
            {name: 'app.login', url: 'login', views: {'content@': {component: 'login'}}},
            {name: 'app.dashboard', url: 'dashboard', views: {'content@': {component: 'dashboard'}}},
            {name: 'app.users', url: 'users', views: {'content@': {component: 'users'}}},
            {name: 'app.sbiDataSetTypes', url: 'sbiDataSetTypes', views: {'content@': {component: 'sbiDataSetTypes'}}},
            {name: 'app.sbiProjects', url: 'sbiProjects', views: {'content@': {component: 'sbiProjects'}}},
            {name: 'app.sbiProjects.detail', url: '/:projectId', views: {'content@': {component: 'sbiProjectDetail'}}},
            {name: 'app.nelsprojects', url: 'nelsprojects', views: {'content@': {component: 'nelsProjects'}}},
            {name: 'app.sbiQuota', url: 'sbiquota', views: {'content@': {component: 'sbiQuotas'}}},
            {name: 'app.sbiQuota.detail', url: '/:quotaId', views: {'content@': {component: 'sbiQuotaDetail'}}},
            {name: 'app.invalid', url: 'invalid', views: {'content@': {component: 'invalid'}}},
            {name: 'app.notfound', url: 'app.notfound', views: {'content@': {component: 'app.notfound'}}},
            {name: 'app.denied', url: 'denied', views: {'content@': {component: 'denied'}}}
        ];

        // Loop over the state definitions and register them
        states.forEach(function (state) {
            $stateProvider.state(state);
        });
    }]);

app.config(function ($httpProvider) {
    $httpProvider.interceptors.push('authInterceptorService');
    $httpProvider.defaults.headers.common['Pragma'] = 'no-cache';
});


app.run(function ($rootScope, $state, $stateParams, $log, nelsAuthService) {

    $rootScope.$state = $state;
    $rootScope.$stateParams = $stateParams;

    nelsAuthService.loadAuthData();

});