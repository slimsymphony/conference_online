package frank.incubator.onlineConference.persist;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

import frank.incubator.onlineConference.model.Topic;
import frank.incubator.onlineConference.model.TopicRecord;

@Repository
public class TopicManagerImpl implements TopicManager {

	private JdbcTemplate jt;

	@Inject
	public void setDataSource(DataSource ds) {
		jt = new JdbcTemplate(ds);
	}

	public void addTopic(Topic topic) {
		int count = jt.queryForInt(
				"select count(1) from TOPIC where TOPIC_NAME=?",
				new Object[] { topic.getTopicName() });
		if (count == 0)
			jt.update(
					"insert into TOPIC(TOPIC_NAME,BEGIN_DATE,`DESC`) values(?,?,?)",
					new Object[] { topic.getTopicName(), new Date(),
							topic.getDesc() });
	}

	public void addTopicRecords(List<TopicRecord> lists) {
		for (TopicRecord tr : lists) {
			jt.update(
					"insert into topic_records(TOPIC_NAME,USER,TIME,CONTENT) values(?,?,?,?)",
					new Object[] { tr.getTopicName(), tr.getUser(),
							tr.getTime(), tr.getContent() });
		}
	}

	public List<Topic> getHistoryTopics() {
		final List<Topic> list = new ArrayList<Topic>();
		jt.query("select * from TOPIC", new RowCallbackHandler() {
			public void processRow(ResultSet rs) throws SQLException {
				Topic topic = new Topic();
				topic.setTopicName(rs.getString("TOPIC_NAME"));
				topic.setBeginTime(rs.getTimestamp("BEGIN_DATE"));
				topic.setDesc(rs.getString("DESC"));
				list.add(topic);
			}
		});
		return list;
	}

	public List<TopicRecord> getTopicRecords(final String topicName) {
		final List<TopicRecord> list = new ArrayList<TopicRecord>();
		jt.query("select * from TOPIC_RECORDS where topic_name=?",
				new Object[] { topicName }, new RowCallbackHandler() {
					public void processRow(ResultSet rs) throws SQLException {
						TopicRecord tr = new TopicRecord();
						tr.setTopicName(topicName);
						tr.setTime(rs.getTimestamp("TIME"));
						tr.setUser(rs.getString("USER"));
						tr.setContent(rs.getString("CONTENT"));
						list.add(tr);
					}
				});
		return list;
	}

}
