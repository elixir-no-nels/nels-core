'user strict'
app.controller('editBlockQuotaModalController', function ($log, $uibModalInstance, nelsNavigatorService, blockQuota, $filter) {
    nelsNavigatorService.requireLogin();

    var self = this;


    self.$onInit = function () {
        self.modalTitle = "Edit block quota";
        self.value = blockQuota.value;
        var convertedSize= $filter('bytes')(blockQuota.value, 1);
        var convertedArr;
        if (typeof convertedSize === 'undefined') {
            self.converted_quota_size = 10;
            self.unit = 'GB';
        } else {
            convertedArr = convertedSize.split(' ');
            self.converted_quota_size = Number(parseInt(convertedArr[0]).toFixed());
            self.unit = convertedArr[1];
        }


    };

    self.cancelClick = function () {
        $uibModalInstance.dismiss('cancel');
    };

    self.okClick = function () {
        var size = $filter('magnitude')(self.converted_quota_size, self.unit);
        var data = {"value": size, "comment": self.comment};
        $uibModalInstance.close(data);
    };

    self.isClickable = function () {
        return self.converted_quota_size && self.converted_quota_size > 0 && self.comment;
    }
});