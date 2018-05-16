package no.nels.idp.core.utilities;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class IdpSpringUtilities {
	static ApplicationContext context = new ClassPathXmlApplicationContext(
			"spring/config/idp.xml");

	public static ApplicationContext getSpringApplicationContenxt() {
		return context;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T getSpringBean(String beanName){
		return (T)context.getBean(beanName);
	}
}
