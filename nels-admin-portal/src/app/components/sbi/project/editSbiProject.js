'user strict'
app.controller('editSbiProjectModalController', function (
    $log, NeLSAppSettings, nelsUtilsService, selectionService, userPersistenceService,
    paginationService, $uibModalInstance, nelsNavigatorService, project) {
    nelsNavigatorService.requireLogin();

    var self = this;

    self.$onInit = function () {
        self.modalTitle = "Edit project";
        self.name = project.name;
        self.description = project.description;
        self.contact_person = project.contact_person;
        self.contact_email = project.contact_email;
        self.contact_affiliation = project.contact_affiliation;
    };
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
