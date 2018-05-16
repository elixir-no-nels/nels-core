'use strict';

/* alert service */
app.service('nelsAlertService', function ($rootScope, $log) {
    var self = this;

    var alert = {
        status: "",
        message: "",
        css: "",
        show: false,
        isModal: false
    };

    this.alert = alert;

    this.setAlert = function (status, msg, css) {
        self.alert.message = msg;
        self.alert.css = css;
        self.alert.status = status;
        self.alert.show = true;
    };

    this.setAlertInModal = function (status, msg, css) {
        self.alert.message = msg;
        self.alert.css = css;
        self.alert.status = status;
        self.alert.isModal = true;
    };

    this.showErrorInModal = function (msg) {
        self.setAlertInModal("Error", msg, "alert-danger");
    };

    this.showSuccess = function (msg) {
        self.setAlert("Success", msg, "alert-success");
    };

    this.showWarning = function (msg) {
        self.setAlert("Warning", msg, "alert-warning");
    };

    this.showDanger = function (msg) {
        self.setAlert("Critical", msg, "alert-danger");
    };

    this.showInfo = function (msg) {
        self.setAlert("Info", msg, "alert-info");
    };

    this.closeAlert = function () {
        self.alert.show = false;
    };

});