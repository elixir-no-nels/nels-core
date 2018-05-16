'use strict';

app.controller('paginationController', function () {

    var self = this;
    self.numbers = [5, 10, 25, 50];

    self.onChange = function () {
        self.change({value: self.selected});
    };
});