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
 * ʵ�ֻ���BayeuxServer�ķ���˹���
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
	 * ��¼��ǰ�����û���Ϣ
	 */
	private final ConcurrentMap<String, Map<String, String>> _members = new ConcurrentHashMap<String, Map<String, String>>();

	/**
	 * ��ʼ����ɺ�
	 */
	@PostConstruct
	public void init() {
		log.info("ConferenceService init over.");
	}

	/**
	 * ���û���ͨ������ӹ��˲��Ŵʵ�DataFilter,ͬʱ��Ȩ�������۵�������Ա����Ȩ�� ��ȨCREATE��Ϊ���û������µ����ۻ��顣
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
	 * �����û�����������������Ϣ��Ϊ�û���/service/members��Ȩ��������
	 * 
	 * @param channel
	 */
	@Configure("/service/members")
	private void configureMembers(ConfigurableServerChannel channel) {
		channel.addAuthorizer(GrantAuthorizer.GRANT_PUBLISH);
		channel.setPersistent(true);
	}

	/**
	 * �������л���������Ϣ�����м�¼�ʹ���
	 * Ȼ����Ϣ��������
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
	 * ��¼�û�������������� ������ʵ�ֶ�cache�ͳ־û���һ���Բ���
	 * 
	 * @param topic
	 * @param user
	 * @param content
	 */
	private void saveTopicContent(String topic, String user, String content) {
		if(user.endsWith("ϵͳ��Ϣ"))
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
	 * �����û���¼��Ϣ
	 * 
	 * @param client
	 * @param message
	 */
	@Listener("/service/members")
	public void handleMembership(ServerSession client, ServerMessage message) {
		Map<String, Object> data = message.getDataAsMap();
		// ��ȡ�û���Ҫ��������ۻ���
		final String topic = ((String) data.get("topic"))
				.substring("/conference/".length());
		// ��ȡ��Ӧ�Ѿ����뻰����û�
		Map<String, String> topicMembers = _members.get(topic);
		// ���û���û�����˵�����´����Ļ���
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
			// ���û��˳�����ʱ��֪ͨ�����û�
			public void removed(ServerSession session, boolean timeout) {
				members.values().remove(session.getId());
				broadcastMembers(topic, members.keySet());
			}
		});
		// ������ɣ�֪ͨ����������Ŀǰ���ߵ������û�
		broadcastMembers(topic, members.keySet());
		// Ϊ�¼����û�������ʷ��Ϣ
		sendHistoryMsg(topic,client);
	}

	/**
	 * Ϊ�¼����û�������ʷ��Ϣ
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
	 * ��ָ�������������������û�������Ϣ
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
	 * ����˽������.���Ҳ�����¼
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
	 * ����˽�½�����Ϣ 1���ͻ�����Ҫʹ��/service/privatechat�ŵ�����, ͬʱ��sendto���Լ�¼��Ҫ���͵����û�
	 * 2���ͻ�����Ҫ�ڽ��ܶ�Ӧ������Ϣʱ���scope���ԣ�ȷ���Ƿ���Ҫ��������Ϣ��˽��
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
