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
	//TopicManager tm = Context.getBean("TopicManager",TopicManager.class);
	List<Topic> lists = (List<Topic>)request.getAttribute("topics");//tm.getHistoryTopics();
%>
<html>
 <head>
  <title> 选择讨论事件 </title>
  <meta http-equiv="content-type" content="text/html; charset=UTF-8" />
  <meta http-equiv="pragma" content="no-cache" />
  <meta http-equiv="cache-control" content="no-cache" />
  <meta http-equiv="expires" content="0" />    
  <script type="text/javascript" src="${pageContext.request.contextPath}/dojo/dojo.js"></script>
  <style type="text/css">
	  button{font-size:8pt;font-family:"微软雅黑"}
	  DIV{font-size:12pt;font-family:"微软雅黑"}
	  th{font-size:11pt;font-family:"微软雅黑"}
	</style>
  <script type="text/javascript">
       var config = {
            contextPath: '${pageContext.request.contextPath}'
        };
       
       
       function join( topic ){
    	   window.location.href="conference.jsp?topic="+encodeURI(topic);
       }
       
       function showDiv(){
    	   dojo.byId('dialog1').style.display = "block";
       }
	   function hidDiv(){
    	   dojo.byId('dialog1').style.display = "none";
       }
       function confirmTopic(){
	   		var topic = dojo.byId("newTopicName").value;
			if(topic==''){
				alert("不能为空!");
				return;
			}	
			join(topic);
	   }
	   
       function init(){
		   var newTopicBtn = dojo.byId("newTopicBtn");
		   dojo.connect(newTopicBtn,"onclick",showDiv);
		   var confirmBtn = dojo.byId("confirmBtn");
		   dojo.connect(confirmBtn,"onclick",confirmTopic);
		   var cancelBtn = dojo.byId("cancelBtn");
		   dojo.connect(cancelBtn,"onclick",hidDiv);
       }
       
       dojo.addOnLoad(init);
       
  </script>
 </head>

 <body>
 	<h2 align="center">选择讨论事件</h2><br/>
	<table width="70%" align="center" border="1">
		<tr>
			<th nowrap="true">讨论事件</th>
			<th nowrap="true">发起时间</th>
			<th nowrap="true">操作</th>
		</tr>
		<% for(Topic topic : lists ){ %>
		<tr>
			<td><%=topic.getTopicName()%></td>
			<td><%=topic.getBeginTime()%></td>
			<td>
				<input type="button" onClick="join('<%=topic.getTopicName()%>')" value="加入"/>
			</td>
		</tr>
		<% } %>
		<tr><td colspan="3" align="center"><button id="newTopicBtn">新建事件讨论</button></td></tr>
	</table> 
	<div id="dialog1" style="display:none;z-index:10;position:absolute;left:250pt;top:120pt;background-color:green;width:400px;height:100px">
    	<table>
		    <tr>
		      <td><label for="newTopicName">请填写新事件讨论名称: </label></td>
		      <td><input type="text" id="newTopicName" /></td>
		    </tr>
		    <tr>
		      <td colspan="2" align="center">
		        <button id="confirmBtn">OK</button>&nbsp;&nbsp;&nbsp;
				<button id="cancelBtn">关闭</button>
			  </td>
		    </tr>
		</table>
    </div> 
 </body>
</html>
