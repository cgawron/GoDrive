'use strict';

var EditorState = {
	CLEAN : 0, // NO CHANGES
	DIRTY : 1, // UNSAVED CHANGES
	SAVE : 2, // SAVE IN PROGRESS
	LOAD : 3, // LOADING
	READONLY : 4
};

google.load('picker', '1');

angular.module('app', ['ngRoute', 'app.filters', 'app.services', 'app.directives', 'ngGo.Board.Service', 'ngGo.Player.Service', 'ngGo.Player.Mode.Edit.Service',
               		'ngGo.Player.Mode.Replay.Service' ])
.constant('saveInterval', 15000)
.constant('appId', '140682578204-8516fe1dt9e7lquhm5v5j39cvpie02kh.apps.googleusercontent.com')
.config(
//		['$routeProvider',
		 function($routeProvider, PlayerProvider, BoardProvider,
				 BoardThemeProvider) {

			// Player configuration
			PlayerProvider.setConfig({
				last_move_marker : 'circle',
				replay_auto_play_delay : 750
			});
			// Board configuration
			BoardProvider.setConfig({
				coordinates : true
			});
			// Board theme
			BoardThemeProvider.setTheme({
				coordinates : {
					vertical : {
						style : 'kanji',
						inverse : false
					},
					horizontal : {
						style : 'numbers',
						inverse : false
					}
				}
			});

			$routeProvider
			.when(
					'/view/',
					{
						templateUrl : 'partials/viewer.html',
						controller : PlayCtrl
					})
					.when(
							'/view/:id',
							{
								templateUrl : 'partials/viewer.html',
								controller : PlayCtrl
							}).otherwise({
								redirectTo : '/view/'
							});
		} );
