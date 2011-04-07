dojo.require("dojox.cometd");
dojo.require("dojox.cometd.timestamp");
dojo.require("dojox.cometd.ack");
dojo.require("dojox.cometd.reload");

var topic = {
    _lastUser: null,
    _username: null,
    _connected: false,
    _disconnecting: false,
    _chatSubscription: null,
    _membersSubscription: null,

    _init: function()
    {
        dojo.removeClass("join", "hidden");
        dojo.addClass("joined", "hidden");
        dojo.byId('username').focus();

        dojo.query("#username").attr({
            "autocomplete": "off"
        }).onkeyup(function(e)
        {
            if (e.keyCode == dojo.keys.ENTER)
            {
                topic.join(dojo.byId('username').value);
            }
        });

        dojo.query("#joinButton").onclick(function(e)
        {
            topic.join(dojo.byId('username').value);
        });

        dojo.query("#phrase").attr({
            "autocomplete": "off"
        }).onkeyup(function(e)
        {
            if (e.keyCode == dojo.keys.ENTER)
            {
                topic.chat();
            }
        });

        dojo.query("#sendButton").onclick(function(e)
        {
            topic.chat();
        });

        dojo.query("#leaveButton").onclick(topic, "leave");
        
        dojo.query("#attachButton").onclick(function(e){
        	alert("here");
        	 var text = dojo.byId('phrase').value;
             text = "$img" + text;
             dojo.byId('phrase').value = text;
             topic.chat();
        });
        
        dojo.query("#clearButton").onclick(topic,"clear");

        /**暂时禁用自动cookie记录找回 便于调试
        // Check if there was a saved application state
        var stateCookie = org.cometd.COOKIE?org.cometd.COOKIE.get('org.cometd.demo.state'):null;
        var state = stateCookie ? org.cometd.JSON.fromJSON(stateCookie) : null;
        // Restore the state, if present
        if (state)
        {
            dojo.byId('username').value=state.username;
            setTimeout(function()
            {
                // This will perform the handshake
                topic.join(state.username);
            }, 0);
        }
        */
    },

    join: function(name)
    {
        topic._disconnecting = false;

        if (name == null || name.length == 0)
        {
            alert('请输入名称');
            return;
        }

        //dojox.cometd.ackEnabled = dojo.query("#ackEnabled").attr("checked");
        //这里配置websocket的方式
        dojox.cometd.websocketEnabled = true;
        var cometdURL = location.protocol + "//" + location.host + config.contextPath + "/cometd";
        dojox.cometd.init({
            url: cometdURL,
            logLevel: "info"
        });

        topic._username = name;

        dojo.addClass("join", "hidden");
        dojo.removeClass("joined", "hidden");
        dojo.byId("phrase").focus();
    },

    _unsubscribe: function()
    {
    	//alert("unsubscribe");
        if (topic._chatSubscription)
        {
            dojox.cometd.unsubscribe(topic._chatSubscription);
        }
        topic._chatSubscription = null;
        if (topic._membersSubscription)
        {
            dojox.cometd.unsubscribe(topic._membersSubscription);
        }
        topic._membersSubscription = null;
    },

    _subscribe: function()
    {
    	//alert("subscribe");
        topic._chatSubscription = dojox.cometd.subscribe('/conference/'+topicName, topic.receive);
        topic._membersSubscription = dojox.cometd.subscribe('/members/'+topicName, topic.members);
    },

    leave: function()
    {
        dojox.cometd.batch(function()
        {
            dojox.cometd.publish("/conference/"+topicName, {
                user: "系统信息",
                membership: 'leave',
                chat: "['"+topic._username + "' 已经离开讨论]"
            });
            topic._unsubscribe();
        });
        dojox.cometd.disconnect();
        // switch the input form
        dojo.removeClass("join", "hidden");
        dojo.addClass("joined", "hidden");

        dojo.byId("username").focus();
        dojo.byId('members').innerHTML = "";

        topic._username = null;
        topic._lastUser = null;
        topic._disconnecting = true;
    },
    
    clear: function()
    {
    	dojo.byId('chat').innerHTML = '';
    },
    
    chat: function()
    {
        var text = dojo.byId('phrase').value;
        dojo.byId('phrase').value = '';
        if (!text || !text.length) return;

        var colons = text.indexOf("::");
        if (colons > 0)
        {
            dojox.cometd.publish("/service/privatechat", {
                topic: "/conference/"+topicName, // This should be replaced by the topic name
                user: topic._username,
                chat: text.substring(colons + 2),
                sendto: text.substring(0, colons)
            });
        }
        else
        {
            dojox.cometd.publish("/conference/"+topicName, {
                user: topic._username,
                chat: text
            });
        }
    },

    receive: function(message)
    {
        var fromUser = message.data.user;
        var membership = message.data.join || message.data.leave;
        var text = message.data.chat;

        if (!membership && fromUser == topic._lastUser)
        {
            //fromUser = "...";
        }
        else
        {
            topic._lastUser = fromUser;
            fromUser += ":";
        }

        var chat = dojo.byId('chat');
        if (membership)
        {
            chat.innerHTML += "<span class=\"membership\"><span class=\"from\">" + fromUser + "&nbsp;</span><span class=\"text\">" + text + "</span></span><br/>";
            topic._lastUser = null;
        }else if (message.data.scope == "private"){
            chat.innerHTML += "<span class=\"private\"><span class=\"from\">" + fromUser + "&nbsp;</span><span class=\"text\">[private]&nbsp;" + text + "</span></span><br/>";
        }else if(text.indexOf("$img")>=0){
        	text = text.substr(4);
        	chat.innerHTML += "<span class=\"from\">" + fromUser + "&nbsp;</span><p><img src='" + text + "'/></p><br/>";
        }else{
            chat.innerHTML += "<span class=\"from\">" + fromUser + "&nbsp;</span><span class=\"text\">" + text + "</span><br/>";
        }

        chat.scrollTop = chat.scrollHeight - chat.clientHeight;
    },

    members: function(message)
    {
        var members = dojo.byId('members');
        var list = "";
        for (var i in message.data)
            list += message.data[i] + "<br/>";
        members.innerHTML = list;
    },

    _connectionInitialized: function()
    {
    	//alert("_connectionInitialized");
        // first time connection for this client, so subscribe and tell everybody.
        dojox.cometd.batch(function()
        {
            topic._subscribe();
            dojox.cometd.publish('/conference/'+topicName, {
                user: "系统信息",
                membership: 'join',
                chat: "['" + topic._username + "']加入讨论"
            });
        });
    },

    _connectionEstablished: function()
    {
    	//alert("_connectionEstablished");
        // connection establish (maybe not for first time), so just
        // tell local user and update membership
        topic.receive({
            data: {
                user: '系统信息',
                chat: '已和服务器建立连接'
            }
        });
        dojox.cometd.publish('/service/members', {
            user: topic._username,
            topic: '/conference/'+topicName
        });
    },

    _connectionBroken: function()
    {
        topic.receive({
            data: {
                user: '系统信息',
                chat: '和服务器的连接出现问题'
            }
        });
        dojo.byId('members').innerHTML = "";
    },

    _connectionClosed: function()
    {
        topic.receive({
            data: {
                user: '系统信息',
                chat: '和服务器的连接已关闭'
            }
        });
    },

    _metaHandshake: function(message)
    {
    	//alert("_metaHandshake");
    	if (message.successful)
        {
            topic._connectionInitialized();
        }
    },

    _metaConnect: function(message)
    {
    	//alert("_metaConnect");
        if (topic._disconnecting)
        {
            topic._connected = false;
            topic._connectionClosed();
        }
        else
        {
            var wasConnected = topic._connected;
            topic._connected = message.successful === true;
            if (!wasConnected && topic._connected)
            {
                topic._connectionEstablished();
            }
            else if (wasConnected && !topic._connected)
            {
                topic._connectionBroken();
            }
        }
    }
};
//设置是否需要启用websocket
//dojox.cometd.websocketEnabled = true;
dojox.cometd.addListener("/meta/handshake", topic, topic._metaHandshake);
dojox.cometd.addListener("/meta/connect", topic, topic._metaConnect);
dojo.addOnLoad(topic, "_init");
dojo.addOnUnload(function()
{
    if (topic._username)
    {
        dojox.cometd.reload();
        //暂时禁用cookie
        /*org.cometd.COOKIE.set('org.cometd.'+topicName+'.state', org.cometd.JSON.toJSON({
            username: topic._username
        }), { 'max-age': 5 });*/
    }
    else
        dojox.cometd.disconnect();
});

