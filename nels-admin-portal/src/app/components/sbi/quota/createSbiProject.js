'user strict'
app.controller('createSbiProjectModalController', function (
    $log, $uibModalInstance, nelsNavigatorService) {
    nelsNavigatorService.requireLogin();

    var self = this;
    self.modalTitle = "Create project";

    self.cancelClick = function () {
        $uibModalInstance.dismiss('cancel');
    };

    self.okClick = function () {
        var data = {"name": self.name, "description": self.description, "contact_person": self.contact_person, "contact_email": self.contact_email, "contact_affiliation": self.contact_affiliation};
        $uibModalInstance.close(data);
    };

    self.isClickable = function () {
        return self.name && self.description;
    }
});
