package no.nels.portal.session;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.utilities.JSFUtils;

@ManagedBean(name = ManagedBeanNames.session_rootRedirectBean)
@SessionScoped
public class RootRedirectBean {

	private boolean hasRootRedirect = false;
	private String url = JSFUtils.getApplicationRoot();

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public boolean getHasRootRedirect() {
		boolean ret = hasRootRedirect;
		//reset it to false because it's handled by the page that called this function
		setHasRootRedirect(false);
		return ret;
	}

	public void setHasRootRedirect(boolean hasRootRedirect) {
		this.hasRootRedirect = hasRootRedirect;
	}

}
