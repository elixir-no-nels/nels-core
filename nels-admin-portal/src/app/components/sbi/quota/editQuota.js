'user strict'
app.controller('editQuotaModalController', function ($log, $uibModalInstance, nelsNavigatorService, quota, $filter) {
    nelsNavigatorService.requireLogin();

    var self = this;

    self.$onInit = function () {
        self.modalTitle = "Edit quota";
        self.name = quota.name;
        self.description = quota.description;
        self.quota_size = quota.quota_size;
        var convertedSize = $filter('bytes')(quota.quota_size, 1);
        var convertedArr = convertedSize.split(' ');
        self.converted_quota_size = Number(parseInt(convertedArr[0]).toFixed());
        self.unit = convertedArr[1];
    };

    self.cancelClick = function () {
        $uibModalInstance.dismiss('cancel');
    };

    self.okClick = function () {
        var size = $filter('magnitude')(self.converted_quota_size, self.unit);
        var data = {"name": self.name, "description": self.description, "quota_size": size};
        $uibModalInstance.close(data);
    };

    self.isClickable = function () {
        return self.name && self.description && self.converted_quota_size && self.converted_quota_size > 0;
    }
});