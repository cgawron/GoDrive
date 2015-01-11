'use strict';

var module = angular.module('app.services', []);

var ONE_HOUR_IN_MS = 1000 * 60 * 60;

// Shared model for current document
module.factory('doc',
    function ($rootScope) {
		var node = null;
        var service = $rootScope.$new(true);
        service.dirty = false;
        service.lastSave = 0;
        service.timeSinceLastSave = function () {
            return new Date().getTime() - this.lastSave;
        };
        service.$watch('info',
            function (newValue, oldValue) {
                if (oldValue != null && newValue === oldValue) {
                    service.dirty = true;
                }
            },
            true);
        return service;
    });

module.factory('viewer',
	    function (doc, backend, $q, $location, $rootScope, $log) {
	        var viewer = null;
	        var service = {
	            loading:false,
	            saving:false,
	            rebind:function (element) {
	                // viewer = ace.edit(element);
	            },
	            snapshot:function () {
	                doc.dirty = false;
	                var data = angular.extend({}, doc.info);
	                data.resource_id = doc.resource_id;
	                if (doc.info.editable) {
	                    // data.content = viewer.getSession().getValue();
	                }
	                return data;
	            },
	            create:function () {
	                $log.info("Creating new doc");
	                var promise = backend.create();
	                promise.then(angular.bind(this,
	                    function (result) {
	                        $log.info("Created file", result);
	                        this.saving = false;
	                        this.updateViewer(result.data);
	                        doc.lastSave = new Date().getTime();
	                        $location.path('view/' + doc.resource_id);
	                        $rootScope.$broadcast('created', doc.info);
	                        return doc.info;
	                    }), angular.bind(this,
	                    function (result) {
	                        this.saving = false;
	                        doc.dirty = true;
	                        $rootScope.$broadcast('error', {
	                            action:'save',
	                            message:"An error occured while saving the file"
	                        });
	                        return result;
	                    }));
	                return promise;
	            },
	            load:function (fileId, reload) {
	                $log.info("Loading resource", id, doc.resource_id);
	                if (!reload && doc.info && fileId == doc.resource_id) {
	                    return $q.when(doc.info);
	                }
	                this.loading = true;
	                return backend.load(fileId).then(angular.bind(this,
	                    function (result) {
	                        this.loading = false;
	                        this.updateViewer(result.data);
	                        $rootScope.$broadcast('loaded', doc.info);
	                        return result;
	                    }), angular.bind(this,
	                    function (result) {
	                        $log.warn("Error loading", result);
	                        this.loading = false;
	                        $rootScope.$broadcast('error', {
	                            action:'load',
	                            message:"An error occured while loading the file"
	                        });
	                        return result;
	                    }));
	            },
	            save:function (newRevision) {
	                $log.info("Saving file", newRevision);
	                if (this.saving || this.loading) {
	                    throw 'Save called from incorrect state';
	                }
	                this.saving = true;
	                var file = this.snapshot();

	                // Force revision if first save of the session
	                newRevision = newRevision || doc.timeSinceLastSave() > ONE_HOUR_IN_MS;
	                var promise = backend.save(file, newRevision);
	                promise.then(angular.bind(this,
	                    function (result) {
	                        $log.info("Saved file", result);
	                        this.saving = false;
	                        doc.resource_id = result.data;
	                        doc.lastSave = new Date().getTime();
	                        $rootScope.$broadcast('saved', doc.info);
	                        return doc.info;
	                    }), angular.bind(this,
	                    function (result) {
	                        this.saving = false;
	                        doc.dirty = true;
	                        $rootScope.$broadcast('error', {
	                            action:'save',
	                            message:"An error occured while saving the file"
	                        });
	                        return result;
	                    }));
	                return promise;
	            },
	            updateViewer:function (fileInfo) {
	                $log.info("Updating viewer", fileInfo);
	                doc.info = fileInfo;
	                doc.resource_id = fileInfo.fileId;
	                backend.node(doc.resource_id, doc.info.rootId).then(angular.bind(this,
		                    function (result) {
		                        this.updateNode(result.data);
		                        return result;
		                    }));
	                //viewer.setSession(session);
	                //viewer.setReadOnly(!doc.info.editable);
	                //viewer.focus();
	            },
	            getStone:function (id) {
	                $log.info("getStone ", id);
	                return doc.node.board[id];
	            },
	            updateNode:function (node) {
	                $log.info("Updating node", node);
	                doc.node = node;
	                $rootScope.$broadcast('nodeChanged', doc.node);
	            },
	            start:function () {
	                $log.info("Start");
	                var nextId = doc.info.rootId;
	                backend.node(doc.resource_id, nextId).then(angular.bind(this,
		                    function (result) {
		                        this.updateNode(result.data);
		                        return result;
		                    }));	                
	            },
	            next:function () {
	                $log.info("Next");
	                if (doc.node.children.length == 0) return;
	                var nextId = doc.node.children[0];
	                backend.node(doc.resource_id, nextId).then(angular.bind(this,
		                    function (result) {
		                        this.updateNode(result.data);
		                        return result;
		                    }));	                
	            },
	            prev:function () {
	                $log.info("Prev");
	                if (doc.node.parent < 0) return;
	                var nextId = doc.node.parent;
	                backend.node(doc.resource_id, nextId).then(angular.bind(this,
		                    function (result) {
		                        this.updateNode(result.data);
		                        return result;
		                    }));	                
	            },
	            end:function () {
	                $log.info("End");
	                var nextId = doc.node.endOfLine;
	                if (nextId === null) return;
	                backend.node(doc.resource_id, nextId).then(angular.bind(this,
		                    function (result) {
		                        this.updateNode(result.data);
		                        return result;
		                    }));	                
	            },
	            play:function (id) {
	            	$log.info('play on ' + id);
	            	if (doc.node.board[id] != null) {
	            		$log.error("illegal move!");
	            		$rootScope.$broadcast('error', {
                            action:'play',
                            message:"You cannot place a stone on top of another stone!"
                        });
	            		return;
	            	}
	            	
	            	backend.play(doc.resource_id, doc.node, id).then(angular.bind(this,
		                    function (result) {
		                        this.updateNode(result.data);
		                        return result;
		                    }));	            
	            },
	            state:function () {
	                if (this.loading) {
	                    return EditorState.LOAD;
	                } else if (this.saving) {
	                    return EditorState.SAVE;
	                } else if (doc.dirty) {
	                    return EditorState.DIRTY;
	                } else if (!doc.info.editable) {
	                    return EditorState.READONLY;
	                }
	                return EditorState.CLEAN;
	            }
	        };
	        return service;
	    });

module.factory('backend',
    function ($http, $log) {
        var jsonTransform = function (data, headers) {
            return angular.fromJson(data);
        };
        var service = {
            user:function () {
                return $http.get('user', {transformResponse:jsonTransform});
            },
            about:function () {
                return $http.get('about', {transformResponse:jsonTransform});
            },
            load:function (id) {
             	$log.info('backend.load, file_id=' + id);
                return $http.get('sgf/info', {
                    transformResponse:jsonTransform,
                    params:{
                        'file_id':id
                    }
                });
            },
            node:function (file_id, node_id) {
            	$log.info('backend.node, file_id=' + file_id + ', node=' + node_id);
                return $http.get('sgf/node', {
                    transformResponse:jsonTransform,
                    params:{
                        'id':node_id,
                        'file_id':file_id
                    }
                });
            },
            play:function (file_id, node, id) {
                $log.info('Play and create new child node');
                return $http({
                    url:'sgf/node',
                    method:'PUT',
                    headers:{
                        'Content-Type':'application/json'
                    },
                    params:{
                        'point':id,
                        'file_id':file_id
                    },
                    transformResponse:jsonTransform,
                    data:JSON.stringify(node)
                });
            },
            create:function () {
                $log.info('Creating new file');
                return $http({
                    url:'sgf/info',
                    method:'PUT',
                    headers:{
                        'Content-Type':'application/json'
                    },
                    transformResponse:jsonTransform,
                    data:""
                });
            },
            save:function (fileInfo, newRevision) {
                $log.info('Saving', fileInfo);
                return $http({
                    url:'svc',
                    method:fileInfo.resource_id ? 'PUT' : 'POST',
                    headers:{
                        'Content-Type':'application/json'
                    },
                    params:{
                        'newRevision':newRevision
                    },
                    transformResponse:jsonTransform,
                    data:JSON.stringify(fileInfo)
                });
            },
            saveNode:function (doc, node) {
                $log.info('Saving', doc, node);
                
                return $http({
                    url:'sgf/node',
                    method:'POST',
                    headers:{
                        'Content-Type':'application/json'
                    },
                    params:{
                        'file_id':doc.info.fileId
                    },
                    transformResponse:jsonTransform,
                    data:JSON.stringify(node)
                });
            },
            saveInfo:function (doc, info) {
                $log.info('Saving', doc, info);
               
                return $http({
                    url:'sgf/node',
                    method:'POST',
                    headers:{
                        'Content-Type':'application/json'
                    },
                    params:{
                        'file_id':doc.info.fileId
                    },
                    transformResponse:jsonTransform,
                    data:JSON.stringify(info)
                });
            }
        };
        return service;
    });


