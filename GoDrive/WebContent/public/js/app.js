'use strict';

var EditorState = {
    CLEAN:0, // NO CHANGES
    DIRTY:1, // UNSAVED CHANGES
    SAVE:2, // SAVE IN PROGRESS
    LOAD:3, // LOADING
    READONLY:4
};

google.load('picker', '1');

angular.module('app', ['ngRoute', 'app.filters', 'app.services', 'app.directives'])
    .constant('saveInterval', 15000)
    .constant('appId', '140682578204-8516fe1dt9e7lquhm5v5j39cvpie02kh.apps.googleusercontent.com') // Please replace this with your Application ID.
    .config(['$routeProvider', function ($routeProvider) {
    $routeProvider
    	.when('/view/', {
    		templateUrl: '/GoDrive/public/partials/viewer.html',
    		controller: PlayCtrl})
        .when('/view/:id', {
            templateUrl: '/GoDrive/public/partials/viewer.html',
            controller: PlayCtrl})
        .otherwise({redirectTo:'/view/'});
}]);
