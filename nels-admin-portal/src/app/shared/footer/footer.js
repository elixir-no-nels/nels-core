'use strict';
/* footer controller */
app.controller('footerController', function () {
    var self = this;

    self.year = function(){
        return new Date().getFullYear();
    }
});