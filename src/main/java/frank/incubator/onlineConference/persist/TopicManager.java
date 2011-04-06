package frank.incubator.onlineConference.persist;

import java.util.List;

import frank.incubator.onlineConference.model.Topic;
import frank.incubator.onlineConference.model.TopicRecord;

public interface TopicManager {
	public void addTopic(Topic topic);
	public void addTopicRecords(List<TopicRecord> lists);
	public List<Topic> getHistoryTopics();
	public List<TopicRecord> getTopicRecords(String topicName);
}
