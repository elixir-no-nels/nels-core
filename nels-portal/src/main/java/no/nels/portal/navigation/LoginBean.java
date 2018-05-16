package no.nels.portal.navigation;


import no.nels.commons.model.NelsUser;
import no.nels.portal.abstracts.ASecureBean;
import no.nels.portal.facades.NavigationFacade;
import no.nels.portal.facades.SecurityFacade;
import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.model.enumerations.TabView;
import no.nels.portal.session.ContentNavigatorBean;
import no.nels.portal.session.UserSessionBean;
import no.nels.portal.utilities.JSFUtils;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

@ManagedBean(name = ManagedBeanNames.navigation_loginBean)
@RequestScoped
public class LoginBean extends  ASecureBean{

	public String goHome() {
		//trigger nels user registration/fetching
		NelsUser nelsUser = JSFUtils.getManagedBean(
				ManagedBeanNames.session_userSessionBean, UserSessionBean.class)
				.getCurrentUser();
		SecurityFacade.setUserBeingViewed(nelsUser);
		JSFUtils.getManagedBean(ManagedBeanNames.session_contentNavigatorBean, ContentNavigatorBean.class).setCurrentFolder("Personal", TabView.NELS);
		NavigationFacade.goHome();

		return "";
	}

	public void secure() {
		SecurityFacade.requireLogin();
	}

	public String getPageTitle() {
		secure();
		goHome();
		return "Login page - redirects to appropriate page";
	}
}
