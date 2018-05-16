'user strict'
app.controller('createSbiDataSetTypeModalController', function (
    $log, $uibModalInstance, nelsNavigatorService, nelsAlertService) {
    nelsNavigatorService.requireLogin();

    var self = this;
    self.modalTitle = "Create Data Set Type";

    self.cancelClick = function () {
        $uibModalInstance.dismiss('cancel');
    };

    self.okClick = function () {
        var isValidated = true;
        var regex = RegExp('[A-Za-z0-9_]+');
        self.subtypes.trim().split(",").forEach(function (element) {
            isValidated = regex.test(element.trim());
        });
        if (isValidated) {
            var data = {"name": self.name, "description": self.description, "subtypes": self.subtypes};
            $uibModalInstance.close(data);
        } else {
            nelsAlertService.showErrorInModal("Subtypes have illegal characters.");
        }
    };

    self.isClickable = function () {
        return self.name && self.description && self.subtypes;
    }
});
