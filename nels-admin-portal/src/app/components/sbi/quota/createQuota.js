'user strict'
app.controller('createQuotaModalController', function ($log, $uibModalInstance, nelsNavigatorService, $filter) {
    nelsNavigatorService.requireLogin();

    var self = this;

    self.$onInit = function () {
        self.modalTitle = "Create quota";
        self.unit = 'GB';
    };

    self.cancelClick = function () {
        $uibModalInstance.dismiss('cancel');
    };

    self.okClick = function () {
        var size = $filter('magnitude')(self.quota_size, self.unit);
        var data = {"name": self.name, "description": self.description, "quota_size": size};
        $uibModalInstance.close(data);
    };

    self.isClickable = function () {
        return self.name && self.description && self.quota_size && self.quota_size > 0;
    }
});