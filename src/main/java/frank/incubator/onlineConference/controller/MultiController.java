package frank.incubator.onlineConference.controller;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import frank.incubator.onlineConference.model.Topic;
import frank.incubator.onlineConference.persist.TopicManager;

@Controller
public class MultiController {
	@Inject
	private TopicManager tm;
	
	@RequestMapping("/index.do")
	public String doGet(ModelMap model) {
		List<Topic> lists = tm.getHistoryTopics();
		model.addAttribute("topics", lists);
		return "chooseTopic.jsp";
	}
}
