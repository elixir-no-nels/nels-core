package no.nels.portal.abstracts;

import no.nels.commons.utilities.StringUtilities;
import no.nels.portal.facades.NavigationFacade;
import no.nels.portal.facades.SecurityFacade;
import no.nels.portal.utilities.JSFUtils;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

public abstract class ANelsBean {

	public abstract String getPageTitle(); // this is a hook to tie a bean with
											// its page
	String url = JSFUtils.getApplicationRoot();

	public String getReferrerUrl() {
		return JSFUtils.getReferrerUrl();
	}

	public String getRequestUrl() {
		return url;
	}

	public String getApplicationRoot() {
		return JSFUtils.getApplicationRoot();
	}

	public void cancelPopup() {
		NavigationFacade.showPage(getReferrerUrl(), false, true);
	}

	// register url parameters for next use - since view scoped url paramters
	// get lost after loading the page
	HashMap<String, Object> urlParams = new HashMap<String, Object>();

	private void addUrlParameter(String paramName, Object obj) {
		urlParams.put(paramName, obj);
	}

	// to keep the original url paramters and re-use them on post backs since
	// JSF doesn't maintain the original request url on post back
	public void registerRequestUrl() {
		HttpServletRequest request = (HttpServletRequest) FacesContext
				.getCurrentInstance().getExternalContext().getRequest();
		Map<String, String[]> params = request.getParameterMap();
		url = request.getRequestURL().toString();
		for (String key : params.keySet()) {
			addUrlParameter(key, request.getParameter(key));
			url = StringUtilities.appendUrlParameter(url, key,
					request.getParameter(key));
		}
	}

	public String getPageMode() {
		return String
				.valueOf(getUrlParameter(no.nels.portal.model.enumerations.URLParameterNames.Mode));
	}

	public Object getUrlParameter(String parameterName) {
		if (urlParams.containsKey(parameterName)) {
			return urlParams.get(parameterName);
		}
		NavigationFacade.showInvalidOperation();
		return null;
	}

	public boolean hasUrlParameter(String parameterName) {
		return urlParams.containsKey(parameterName);
	}

	public boolean isPostback() {
		return FacesContext.getCurrentInstance().isPostback();
	}

	public boolean isUserLoggedIn() {
		return SecurityFacade.isUserLoggedIn();
	}

	public String getNelsParametrizedReferrerUrl() {
		String url = getReferrerUrl();
		for (String key : urlParams.keySet()) {
			url = StringUtilities.appendUrlParameter(url, key,
					urlParams.get(key).toString());
		}
		return url;
	}

}
