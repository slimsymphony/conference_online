package frank.incubator.onlineConference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ConfigurableServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.java.annotation.Configure;
import org.cometd.java.annotation.Listener;
import org.cometd.java.annotation.Service;
import org.cometd.java.annotation.Session;
import org.cometd.server.authorizer.GrantAuthorizer;
import org.cometd.server.filter.DataFilterMessageListener;
import org.cometd.server.filter.NoMarkupFilter;

import frank.incubator.onlineConference.model.Topic;
import frank.incubator.onlineConference.model.TopicRecord;
import frank.incubator.onlineConference.persist.TopicManager;

/**
 * 实现基于BayeuxServer的服务端管理。
 * 
 * @author frank
 * 
 */
@Named // Tells Spring that this is a bean
@Singleton
@Service("conferenceService")
public class ConferenceService {

	private Log log = LogFactory.getLog(ConferenceService.class);

	@Inject
	private BayeuxServer bayeux;
	@Inject
	private TopicManager tm;
	@Session
	private ServerSession serverSession;
	
	private static SimpleDateFormat sdf = new SimpleDateFormat( "<yyyy-MM-dd HH:mm:ss>" );
	
	/**
	 * 记录当前在线用户信息
	 */
	private final ConcurrentMap<String, Map<String, String>> _members = new ConcurrentHashMap<String, Map<String, String>>();

	/**
	 * 初始化完成后
	 */
	@PostConstruct
	public void init() {
		log.info("ConferenceService init over.");
	}

	/**
	 * 配置会议通道，添加过滤不雅词的DataFilter,同时授权参与讨论的所有人员所有权限 授权CREATE是为了用户创建新的讨论会议。
	 * 
	 * @param channel
	 */
	@Configure({ "/conference/*", "/members/*" })
	private void configureConferences(ConfigurableServerChannel channel) {
		DataFilterMessageListener noMarkup = new DataFilterMessageListener(
				bayeux, new NoMarkupFilter(), new BadWordFilter());
		channel.addListener(noMarkup);
		channel.addAuthorizer(GrantAuthorizer.GRANT_ALL);
	}

	/**
	 * 配置用户加入讨论组的相关信息，为用户在/service/members授权，仅授予
	 * 
	 * @param channel
	 */
	@Configure("/service/members")
	private void configureMembers(ConfigurableServerChannel channel) {
		channel.addAuthorizer(GrantAuthorizer.GRANT_PUBLISH);
		channel.setPersistent(true);
	}

	/**
	 * 监听所有会议讨论信息，进行记录和处理。
	 * 然后将信息继续发送
	 * 
	 * @param client
	 * @param message
	 */
	@Listener("/conference/*")
	public void handleTopic(ServerSession client, ServerMessage message) {
		Map<String, Object> data = message.getDataAsMap();
		final String topic = message.getChannel().substring("/conference/".length());
		ClientSessionChannel csc = serverSession.getLocalSession().getChannel(message.getChannel());
		final String userName = (String) data.get("user");
		final String msg = (String) data.get("chat");
		saveTopicContent(topic, userName, msg);
		//csc.publish(data);
	}

	/**
	 * 记录用户发表的讨论内容 本方法实现对cache和持久化的一致性操作
	 * 
	 * @param topic
	 * @param user
	 * @param content
	 */
	private void saveTopicContent(String topic, String user, String content) {
		if(user.endsWith("系统信息"))
			return;
		//System.out.println(topic+">"+user+":"+content);
		TopicRecord tr = new TopicRecord();
		tr.setUser(user);
		tr.setTopicName(topic);
		tr.setContent(content);
		tr.setTime(new Date());
		List<TopicRecord> list = new ArrayList<TopicRecord>();
		list.add(tr);
		tm.addTopicRecords(list);
	}

	/**
	 * 处理用户登录信息
	 * 
	 * @param client
	 * @param message
	 */
	@Listener("/service/members")
	public void handleMembership(ServerSession client, ServerMessage message) {
		Map<String, Object> data = message.getDataAsMap();
		// 获取用户需要加入的讨论话题
		final String topic = ((String) data.get("topic"))
				.substring("/conference/".length());
		// 获取对应已经加入话题的用户
		Map<String, String> topicMembers = _members.get(topic);
		// 如果没有用户，则说明是新创建的话题
		if (topicMembers == null) {
			Map<String, String> new_topic = new ConcurrentHashMap<String, String>();
			topicMembers = _members.putIfAbsent(topic, new_topic);
			if (topicMembers == null)
				topicMembers = new_topic;
			Topic t = new Topic();
			t.setBeginTime(new Date());
			t.setTopicName(topic);
			tm.addTopic(t);
		}
		final Map<String, String> members = topicMembers;
		String userName = (String) data.get("user");
		members.put(userName, client.getId());
		client.addListener(new ServerSession.RemoveListener() {
			// 当用户退出讨论时，通知其他用户
			public void removed(ServerSession session, boolean timeout) {
				members.values().remove(session.getId());
				broadcastMembers(topic, members.keySet());
			}
		});
		// 加入完成，通知该讨论组中目前在线的所有用户
		broadcastMembers(topic, members.keySet());
		// 为新加入用户推送历史信息
		sendHistoryMsg(topic,client);
	}

	/**
	 * 为新加入用户推送历史信息
	 * @param topic
	 * @param client
	 */
	private void sendHistoryMsg(String topic,ServerSession client){
		ServerMessage.Mutable forward = null;
		List<TopicRecord> list = tm.getTopicRecords(topic);
		for(TopicRecord tr : list) {
			forward = bayeux.newMessage();
			Map<String, Object> chat = new HashMap<String, Object>();
			String content = tr.getContent();
			if(content.startsWith("$img"))
				content = "<img src='"+content.substring(4)+"' />";
			chat.put("chat", sdf.format(tr.getTime())+ content);
			chat.put("user", ">>>"+tr.getUser());
			forward.setChannel("/conference/" + topic);
			forward.setId(client.getId());
			forward.setData(chat);
			client.deliver(serverSession, forward);
		}
	}
	
	/**
	 * 向指定讨论组中所有在线用户发布消息
	 * 
	 * @param topic
	 * @param members
	 */
	private void broadcastMembers(String topic, Set<String> members) {
		ClientSessionChannel channel = serverSession.getLocalSession()
				.getChannel("/members/" + topic);
		channel.publish(members);
	}

	/**
	 * 配置私下聊天.并且不被记录
	 * 
	 * @param channel
	 */
	@Configure("/service/privatechat")
	private void configurePrivateChat(ConfigurableServerChannel channel) {
		DataFilterMessageListener noMarkup = new DataFilterMessageListener(
				bayeux, new NoMarkupFilter(), new BadWordFilter());
		channel.setPersistent(true);
		channel.addListener(noMarkup);
		channel.addAuthorizer(GrantAuthorizer.GRANT_PUBLISH);
	}

	/**
	 * 监听私下交流信息 1、客户端需要使用/service/privatechat信道发送, 同时用sendto属性记录需要发送到的用户
	 * 2、客户端需要在接受对应议题信息时检查scope属性，确认是否需要标明该信息是私信
	 * 
	 * @param client
	 * @param message
	 */
	@Listener("/service/privatechat")
	public void privateChat(ServerSession client, ServerMessage message) {
		Map<String, Object> data = message.getDataAsMap();
		String topic = ((String) data.get("topic")).substring("/conference/"
				.length());
		Map<String, String> membersMap = _members.get(topic);
		if (membersMap == null) {
			Map<String, String> new_topic = new ConcurrentHashMap<String, String>();
			membersMap = _members.putIfAbsent(topic, new_topic);
			if (membersMap == null)
				membersMap = new_topic;
		}
		String[] sendtoNames = ((String) data.get("sendto")).split(",");
		ArrayList<ServerSession> sendtos = new ArrayList<ServerSession>(
				sendtoNames.length);

		for (String sendtoName : sendtoNames) {
			String sendtoId = membersMap.get(sendtoName);
			if (sendtoId != null) {
				ServerSession sendto = bayeux.getSession(sendtoId);
				if (sendto != null)
					sendtos.add(sendto);
			}
		}

		if (sendtos.size() > 0) {
			Map<String, Object> chat = new HashMap<String, Object>();
			String text = (String) data.get("chat");
			chat.put("chat", text);
			chat.put("user", data.get("user"));
			chat.put("scope", "private");
			ServerMessage.Mutable forward = bayeux.newMessage();
			forward.setChannel("/conference/" + topic);
			forward.setId(message.getId());
			forward.setData(chat);

			// test for lazy messages
			if (text.lastIndexOf("lazy") > 0)
				forward.setLazy(true);

			for (ServerSession sendto : sendtos)
				if (sendto != client)
					sendto.deliver(serverSession, forward);
			client.deliver(serverSession, forward);
		}
	}
}
