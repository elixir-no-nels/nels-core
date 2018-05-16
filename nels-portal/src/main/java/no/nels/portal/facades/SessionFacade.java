package no.nels.portal.facades;

import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.session.UserSessionBean;
import no.nels.portal.utilities.JSFUtils;

public class SessionFacade {

	private static UserSessionBean getUserSession() {
		return JSFUtils
				.getManagedBean(ManagedBeanNames.session_userSessionBean,
						UserSessionBean.class);
	}

	private static String getItemKey(String key, long itemId) {
		return key + "-" + Long.toString(itemId);
	}

	public static boolean setSessionObject(String key, Object obj) {
		return getUserSession().setSessionObject(key, obj);
	}

	public static boolean setSessionObject(String key, long itemId, Object obj) {
		return setSessionObject(getItemKey(key, itemId), obj);
	}

	public static Object getSessionObject(String key) {
		return getUserSession().getSessionObject(key);
	}

	public static Object getSessionObject(String key, long itemId) {
		return getSessionObject(getItemKey(key, itemId));
	}

	public static boolean isSessionObjectSet(String key) {
		return getUserSession().isSessionObjectSet(key);
	}

	public static boolean isSessionObjectSet(String key, long itemId) {
		return isSessionObjectSet(getItemKey(key, itemId));
	}

	public static boolean removeSessionObject(String key) {
		return getUserSession().removeSessionObject(key);
	}

	public static boolean removeSessionObject(String key, long itemId) {
		return removeSessionObject(getItemKey(key, itemId));
	}

}
