'use strict';

var module = angular.module('app.directives', []);

var max = 18;

function getId(i, j) {
    var x = String.fromCharCode(97 + i);
    var y = String.fromCharCode(97 + j);
    return x+y;
}

var hoshi = new Set([3, 9, 15]);

function getClass(i, j) {
	if (i == 0 && j == 0)
		return "ul";
	else if (i == 0 && j == max)
		return "ur";
	else if (i == max && j == 0)
		return "bl";
	else if (i == max && j == max)
		return "br";
	else if (i == 0)
		return "u";
	else if (i == max)
		return "d";
	else if (j == 0)
		return "l";
	else if (j == max)
		return "r";
	else if (hoshi.has(i) && hoshi.has(j))
		return "h";
	else
		return "";
}


module.directive('gobanUi', function(viewer) {
	return {
	    template: function template(element, attrs) {
		var table = "<table class='goban' cellspacing='0' cellpadding='0'>";
		for (var i=0; i<19; i++) {
		    table += "<tr>";
		    for (var j=0; j<19; j++) {
			var id = getId(i, j);
			var clazz = "";
			var stone = null;
			if (this.node != null) {
				stone = this.node.board[id];
			}
			if (stone != null) {
			    clazz = stone.render();
			}
			else {
			    clazz = getClass(i, j);
			}
			table += "<td id='" + id + "' class='" + clazz + "' ng-click='click(\"" + id + "\")'></td>";
                    }
		    table += "</tr>";
		}
		table += "</table>";
		return table;
	    }
	  };
    });

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
				data = data.replace(/\+[rR]/, " by resignation");
				data = data.replace(/\+[tT]/, " on time");
				data = data.replace(/\+[fF]/, " by forfeit");
				data = data.replace(/\+/, "");
				console.log("result formatter: " + data);
				return data; 
			});
		}
	}
});

nnnmodule.directive('star',
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