'use strict';

app.service('nelsConfirmActionService', function ($window, $uibModal) {
    var self = this;

    self.showConfirmDialog = function (modalOptions) {
        var size = 'sm';
        var modalInstance = $uibModal.open({
            animation: true,
            templateUrl: 'app/shared/core/confirmAction.html',
            controller: 'nelsConfirmActionController',
            size: size,
            resolve: {
                modalOptions: function () {
                    return modalOptions;
                }
            }
        });

        return modalInstance;
    };
});

app
    .controller(
        'nelsConfirmActionController',
        function ($scope, $uibModalInstance, nelsUtilsService,
                  modalOptions) {
            $scope.modalOptions = ((typeof (modalOptions) !== 'undefined') && (modalOptions !== null)) ? modalOptions
                : {
                    headerText: 'Confirm Action',
                    bodyText: 'Proceed ?',
                    actionButtonText: 'Yes',
                    closeButtonText: 'No'
                };

            $scope.cancelClick = function () {
                $uibModalInstance.dismiss('cancel');
            };

            $scope.okClick = function () {
                $uibModalInstance.close(true);
            };

            $scope.nelsUtilsService = nelsUtilsService;
        });

