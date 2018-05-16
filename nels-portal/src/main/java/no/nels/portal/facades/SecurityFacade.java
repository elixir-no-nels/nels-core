package no.nels.portal.facades;

import no.nels.client.UserApi;
import no.nels.commons.abstracts.ASystemUser;
import no.nels.commons.model.IDPUser;
import no.nels.commons.model.NelsUser;
import no.nels.commons.model.systemusers.AdministratorUser;
import no.nels.commons.model.systemusers.HelpDeskUser;
import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.navigation.UserNavigationBean;
import no.nels.portal.session.UserSessionBean;
import no.nels.portal.utilities.JSFUtils;

import java.util.ArrayList;

public class SecurityFacade {

	public static void createNeLSProfile(IDPUser newIdpUser) {
		try {
			NelsUser newNeLSUser = UserApi.registerUser(newIdpUser);
			LoggingFacade
					.logDebugInfo("new nels profile registered: "
							+ Long.toString(newNeLSUser.getId())
							+ " name: "
							+ newNeLSUser.getIdpUser()
							.getFullname());
			if (no.nels.client.Admin.createUser(
					newNeLSUser.getId(), newNeLSUser.getId(),
					newNeLSUser.getIdpUser().getFullname(),
					(int) newNeLSUser.getSystemUserType().getId())) {
				LoggingFacade
						.logDebugInfo("new user registered to storage service. nelsid: "
								+ Long.toString(newNeLSUser.getId())
								+ " name: "
								+ newNeLSUser.getIdpUser()
								.getFullname());
			} else {
				LoggingFacade
						.logDebugInfo("new nels user registration failed on storage service. nelsid: "
								+ Long.toString(newNeLSUser.getId())
								+ " name: "
								+ newNeLSUser.getIdpUser()
								.getFullname());
			}
		} catch (Exception ex) {
			LoggingFacade.logDebugInfo(ex);
		}
	}


	public static NelsUser getLoggedInUser() {
		UserSessionBean ub = JSFUtils
				.getManagedBean(ManagedBeanNames.session_userSessionBean,
						UserSessionBean.class);
		return ub == null ? null : ub.getCurrentUser();
	}

	public static UserSessionBean getUserSessionBean() {
		return JSFUtils
				.getManagedBean(ManagedBeanNames.session_userSessionBean,
						UserSessionBean.class);

	}

	public static boolean isUserLoggedIn() {
		return getLoggedInUser() != null;
	}
	
	public static boolean isUserAdmin(){
		if(isUserLoggedIn()){
			return getLoggedInUser().getSystemUserType().equals(new AdministratorUser());
		}
		return false;
	}
	
	public static boolean isUserHelpDesk(){
		if(isUserLoggedIn()){
			return getLoggedInUser().getSystemUserType().equals(new HelpDeskUser());
		}
		return false;
	}

	public static void requireLogin(boolean requireTermsOfUsage) {
		if (!isUserLoggedIn()) {
			NavigationFacade.showAccessDenied();
		}
		if (requireTermsOfUsage) {
			UserSessionBean userSessionBean = getUserSessionBean();
			if (!userSessionBean.isTermsOfUsageAccepted()) {
				JSFUtils.getManagedBean(ManagedBeanNames.navigation_userBean, UserNavigationBean.class).viewUserTerms();
			}
		}
	}

	public static void requireLogin() {
		requireLogin(true);
	}

	public static void requireSystemUserType(final ASystemUser systemUser) {
		ArrayList<ASystemUser> systemUsers = new ArrayList<ASystemUser>() {
			{
				add(systemUser);
			}
		};
		requireSystemUserType(systemUsers);
	}

	public static void requireSystemUserType(ArrayList<ASystemUser> systemUsers) {
		requireLogin();
		boolean isOk = false;
		ASystemUser userType = getLoggedInUser().getSystemUserType();
		for (ASystemUser sysUser : systemUsers) {
			if (userType.equals(sysUser)) {
				isOk = true;
				break;
			}
		}
		if (!isOk) {
			NavigationFacade.showAccessDenied();
		}
	}

	public static NelsUser getUserBeingViewed() {
		return getUserSessionBean().getUserBeingViewed();
	}

	public static void setUserBeingViewed(NelsUser userBeingViewed) {
		getUserSessionBean().setUserBeingViewed(userBeingViewed);
	}

	public static void setUserBeingViewed(long nelsId) {
		setUserBeingViewed(UserApi.getNelsUserById(nelsId));
	}
}
