'use strict';

var module = angular.module('app.directives', []);


module.directive('viewer',
	    function (viewer) {
	        return {
	            restrict:'A',
	            link:function (scope, element) {
	                viewer.rebind(element[0]);
	            }
	        };
	    });

module.directive('content', function() {
	return {
		restrict : 'A',
		link : function(scope, element) {

		}
	};
});

module.directive('moveNumber', function() {
	return {
		require : 'ngModel',
		link : function(scope, element, attrs, ngModelController) {
			ngModelController.$parsers.push(function(data) {
				// convert data from view format to model format
				console.log("move number: " + data);
				scope.Player.goto(parseInt(data));
				data = scope.Game.path.getMove();
				ngModelController.$setPristine();
				return data; 
			});

			ngModelController.$formatters.push(function(data) {
				console.log("move number: " + data);

				return data; 
			});
		}
	}
});

module.directive('result', function() {
	return {
		require : 'ngModel',
		link : function(scope, element, attrs, ngModelController) {
			ngModelController.$parsers.push(function(data) {
				// convert data from view format to model format
				console.log("result formatter: " + data);
				// convert data from model format to view format
				data = data.replace(/^[Bb]lack wins/, "B+");
				data = data.replace(/^[Ww]hite wins/, "W+");
				data = data.replace(/\s*by\s+([\d\.]+)\s+points/, "$1");
				data = data.replace(/\s*by\s+resignation/, "R");
				data = data.replace(/\s*on\s+[Ttime]/, "T");
				data = data.replace(/\s*by\s+forfeit/, "F");
				console.log("result formatter: " + data);
				return data; // converted
			});

			ngModelController.$formatters.push(function(data) {
				console.log("result formatter: " + data);
				// convert data from model format to view format
				data = data.replace(/^[bB]/, "Black wins");
				data = data.replace(/^[wW]/, "White wins");
				data = data.replace(/\+([\d\.]+)/, " by $1 points");
				data = data.replace(/\+[rR](esign)*/, " by resignation");
				data = data.replace(/\+[tT]/, " on time");
				data = data.replace(/\+[fF]/, " by forfeit");
				data = data.replace(/\+/, "");
				console.log("result formatter: " + data);
				return data; 
			});
		}
	}
});

module.directive('star',
    function () {
        return {
            restrict:'E',
            replace:true,
            scope:{
                val:'=value',
                // Value bound to
                eventFn:'&click'
                // Optional expression evaluated on click
            },
            link:function (scope, element) {
                element.bind('click',
                    function () {
                        scope.$apply(function () {
                            scope.val = !scope.val;
                        });
                        scope.$eval(scope.eventFn, scope.val);
                    });
            },
            template:'<i class="star" ng-class="{\'icon-star\' : val, \'icon-star-empty\' : !val}" ng-click="toggle()"></i>'
        }
    });

module.directive('alert',
    function ($rootScope) {
        return {
            restrict:'E',
            replace:true,
            link:function (scope, element) {
                $rootScope.$on('error',
                    function (event, data) {
                        scope.message = data.message;
                        element.show();
                    });
                scope.close = function () {
                    element.hide();
                };
            },
            template:'<div ng-hide class="alert alert-danger">' +
                '  <span class="close" ng-click="close()">×</span>' +
                '  {{message}}' +
                '</div>'
        }
    })