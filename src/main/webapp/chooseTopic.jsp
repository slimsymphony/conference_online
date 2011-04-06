<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.*" %>
<%@ page import="frank.incubator.onlineConference.*" %>
<%@ page import="frank.incubator.onlineConference.model.*" %>
<%@ page import="frank.incubator.onlineConference.persist.*" %>
<%
	response.setHeader("Cache-Control", "no-store");
	response.setHeader("Pragma", "no-cache");
	response.setDateHeader("Expires", 0);
	request.setCharacterEncoding("UTF-8");
%>
<%
	TopicManager tm = Context.getBean("TopicManager",TopicManager.class);
	List<Topic> lists = tm.getHistoryTopics();
%>
<html>
 <head>
  <title> 选择讨论事件 </title>
  <meta http-equiv="content-type" content="text/html; charset=UTF-8" />
  <meta http-equiv="pragma" content="no-cache" />
  <meta http-equiv="cache-control" content="no-cache" />
  <meta http-equiv="expires" content="0" />    
  <script type="text/javascript" src="${pageContext.request.contextPath}/dojo/dojo.js"></script>
  <script type="text/javascript">
  	   dojo.require("dijit.Dialog");
  	   dojo.require("dijit.form.TextBox");
  	   dojo.require("dijit.form.Button");
       var config = {
            contextPath: '${pageContext.request.contextPath}'
        };
       
       function init(){
    	   
       }
       
       function join( topic ){
    	   window.location.href="conference.jsp?topic="+encodeURI(topic);
       }
       
       function newTopic(){
    	   dijit.byId('dialog1').show();
       }
       
       dojo.addOnLoad(init);
       var nBtn = dojo.byId("newBtn");
       dojo.connect(nBtn,newTopic);
  </script>
 </head>

 <body>
 	<div dojoType="dijit.Dialog" id="dialog1" title="First Dialog"
    	execute="alert('submitted w/args:\n' + dojo.toJson(arguments[0], true));">
    	<table>
		    <tr>
		      <td><label for="name">请填写新事件讨论名称: </label></td>
		      <td><input dojoType="dijit.form.TextBox" type="text" name="name" id="name"></td>
		    </tr>
		    <tr>
		      <td colspan="2" align="center">
		        <button dojoType="dijit.form.Button" type="submit">OK</button></td>
		    </tr>
		</table>
    </div>
	<table width="70%" align="center">
		<tr>
			<td nowrap="true">讨论事件</td>
			<td nowrap="true">发起时间</td>
			<td nowrap="true">操作</td>
		</tr>
		<% for(Topic topic : lists ){ %>
		<tr>
			<td><%=topic.getTopicName()%></td>
			<td><%=topic.getBeginTime()%></td>
			<td>
				<input id="btnJoin" onClick="join(<%=topic.getTopicName()%>)" value="加入"/>
			</td>
		</tr>
		<% } %>
		<tr><td colspan="3" align="center"><input id="newBtn" value="新建事件讨论"/></td></tr>
	</table>  
 </body>
</html>
