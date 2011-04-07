<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ page contentType="text/html; charset=UTF-8" %>
<%
	response.setHeader("Cache-Control", "no-store");
	response.setHeader("Pragma", "no-cache");
	response.setDateHeader("Expires", 0);
	request.setCharacterEncoding("UTF-8");
	String topic = "demo";
	//out.println(request.getParameter("topic"));
	//if( request.getParameter("topic") != null )
	//	topic = new String(request.getParameter("topic").getBytes("ISO-8859-1"),"UTF-8");
	//out.println(topic);
	topic = request.getParameter("topic");
%>
<html>

<head>
    <title>专题事件讨论</title>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<script type="text/javascript">
		var topicName = '<%=topic%>';
	</script>
    <script type="text/javascript" src="${pageContext.request.contextPath}/dojo/dojo.js"></script>
	<script type="text/javascript" src="conference.js"></script>
    <link rel="stylesheet" type="text/css" href="conference.css"></link>
    <script type="text/javascript">
       var config = {
            contextPath: '${pageContext.request.contextPath}'
        };
    </script>
</head>

<body>

<h1>专题事件讨论: <%=topic%></h1>

<div id="topicroom">
    <div id="chat"></div>
    <div id="members"></div>
    <div id="input">
        <div id="join">
            <table>
                <tbody>
                <!--tr>
                    <td>
                        <input id="ackEnabled" type="checkbox" />
                    </td>
                    <td style="display:none;">
                        <label for="ackEnabled">Enable ack extension</label>
                    </td>
                    <td>&nbsp;</td>
                    <td>&nbsp;</td>
                </tr-->
                <tr>
                    <td>&nbsp;</td>
                    <td>
                        	请输入你的名字
                    </td>
                    <td>
                        <input id="username" type="text" />
                    </td>
                    <td>
                        <button id="joinButton" class="button">加入讨论</button>
                    </td>
                </tr>
                </tbody>
            </table>
        </div>
        <div id="joined">
                                主题:
            &nbsp;
            <input id="phrase" type="text" />
            <button id="sendButton" class="button">发送</button>
            <button id="leaveButton" class="button">离开</button>
            <button id="attachButton" class="button">图片</button>
			<button id="clearButton" class="button">清空讨论区</button>
        </div>
    </div>
</div>
<br />
<div style="padding: 0.25em;">提示: 使用 接收人[,接收人2...]::信息 的格式来发送私信</div>

</body>

</html>
