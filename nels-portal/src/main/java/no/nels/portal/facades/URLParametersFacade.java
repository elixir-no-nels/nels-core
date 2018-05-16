package no.nels.portal.facades;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

public class URLParametersFacade {
	public static void requireURLParameter(String parameterName) {
		if (!isUrlParameterSet(parameterName)) {
			NavigationFacade.showInvalidOperation();
		}
	}

	public static boolean isUrlParameterSet(String parameterName) {
		try {
			if (getURLParameter(parameterName) != null) {
				return !getURLParameter(parameterName).equalsIgnoreCase("");
			}
		} catch (Exception ex) {
		}
		return false;
	}

	public static String getURLParameter(String parameterName) {
		HttpServletRequest request = (HttpServletRequest) FacesContext
				.getCurrentInstance().getExternalContext().getRequest();
		return request.getParameter(parameterName);
	}

	public static String getMustUrLParameter(String parameterName) {
		String ret = getURLParameter(parameterName);
		if (ret == null) {
			NavigationFacade.showInvalidOperation();
		}
		return ret;
	}

	public static Optional<String> getURLParameterValue(String parameterName) {
		Optional<String> ret = Optional.empty();
		String value =  getURLParameter(parameterName);
		if(null != value){
			ret = Optional.of(value);
		}
		return ret;

	}

	public static long getNumberURLParameter(String parameterName) {
		long ret = -1;
		String paramString = getMustUrLParameter(parameterName);
		try {
			ret = Long.parseLong(paramString);
		} catch (Exception ex) {
			NavigationFacade.showInvalidOperation();
		}
		return ret;
	}

	public static long getIDParameter() {
		return getNumberURLParameter(no.nels.portal.model.enumerations.URLParameterNames.ID);
	}

	public static String getPageMode() {
		return getMustUrLParameter(no.nels.portal.model.enumerations.URLParameterNames.Mode);
	}

}
