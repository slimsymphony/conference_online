package frank.incubator.onlineConference;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Context {
	
	private static AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();    
	public static <T>T getBean(String beanName, Class<T> clazz){
		return ctx.getBean(beanName,clazz);
	}
}
