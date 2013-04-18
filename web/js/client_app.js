(function DemoViewModel() {

    var that = this;
    var eb = new vertx.EventBus(window.location.protocol + '//' + window.location.hostname + ':' + window.location.port + '/eventbus');

    eb.onopen = function() {
        eb.send('vertx.mongopersistor', {
            action: 'find',
            collection: 'messages',
            matcher: {}
        }, function(reply) {
            if (reply.status === 'ok') {
                var msgArray = [];
                for (var i = 0; i < reply.results.length; i++) {
                    msgArray[i] = new Message(reply.results[i]);
                }
                that.msgs = ko.observableArray(msgArray);
                ko.applyBindings(that);
            } else {
                console.error('Failed to retrieve messages: ' + reply.message);
            }
        });
    }


    eb.onclose = function() {
        eb = null;
    };

    that.submitMsg = function() {
        var message = ko.toJS(that.message);
        var twitterMsg = {
            sessionID: that.sessionID(),
            action: "save",
            collection: "messages",
            document: {
                user: that.username(),
                text: message
            }
        }

        eb.send('vertx.broadcaster', twitterMsg, function(reply) {
            if (reply.status === 'ok') {
                console.log('Message persisted successfully....');
            } else {
                console.error('Failed to accept order');
            }
        });
    };

    that.username = ko.observable('');
    that.password = ko.observable('');
    that.sessionID = ko.observable('');
    that.message = ko.observable('');

    that.login = function() {
        if (that.username().trim() != '' && that.password().trim() != '') {
            eb.send('vertx.basicauthmanager.login', {
                username: that.username(),
                password: that.password()
            }, function(reply) {
                if (reply.status === 'ok') {
                    that.sessionID(reply.sessionID);
                    if (eb) {
                        eb.registerHandler(that.username().trim(), function(msg, replyTo) {
                            that.msgs.push(new Message(msg));
                            console.log(msg);
                        });
                    }
                    // Get the static data
                } else {
                    alert('invalid login');
                }
            });
        }
    }

    function Message(json) {
        var that = this;
        that._id = json._id;
        that.user = json.user;
        that.text = json.text;
    }
})();
