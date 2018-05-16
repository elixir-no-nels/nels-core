package no.nels.portal.pages;

import no.nels.client.ProjectApi;
import no.nels.client.UserApi;
import no.nels.commons.abstracts.ASystemUser;
import no.nels.commons.model.systemusers.AdministratorUser;
import no.nels.commons.model.systemusers.HelpDeskUser;
import no.nels.idp.core.facades.IdpFacade;
import no.nels.portal.abstracts.ASecureBean;
import no.nels.portal.facades.SecurityFacade;
import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.navigation.IdpNavigationBean;
import no.nels.portal.navigation.ProjectNavigationBean;
import no.nels.portal.navigation.UserNavigationBean;
import no.nels.portal.utilities.JSFUtils;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import java.util.ArrayList;

@ManagedBean(name = ManagedBeanNames.pages_admin_home)
@RequestScoped
public class AdminHomeBean extends ASecureBean {

	private long usersCount = UserApi.getNelsUsersCount();

	public void secure() {
		ArrayList<ASystemUser> userTypes = new ArrayList<ASystemUser>() {
			{
				add(new AdministratorUser());
				add(new HelpDeskUser());
			}
		};
		SecurityFacade.requireSystemUserType(userTypes);
	}

	public String getPageTitle() {
		if (!isPostback()) {
			secure();
			this.registerRequestUrl();
			checkPickerReturn();
		}
		return "NeLS Admin Home";
	}

	public void checkPickerReturn() {
	}

	public String getViewFeideUsersLabel() {
		long usersCount = UserApi.getNelsUsersCount();
		return usersCount > 0 ? "View (" + String.valueOf(usersCount)
				+ ")" : "View";
	}

    public String getViewProjectsLabel() {
        long projectsCount = ProjectApi.getProjectsCount();
        return projectsCount > 0 ? "View (" + String.valueOf(projectsCount)
                + ")" : "View";
    }

	public void cmdViewFeideUsers_Click(String closeJs) {
		JSFUtils.getManagedBean(ManagedBeanNames.navigation_userBean,
				UserNavigationBean.class).editUsers(closeJs);
	}

    public void cmdViewProjects_Click(String closeJs) {
        JSFUtils.getManagedBean(ManagedBeanNames.navigation_projectBean,
                ProjectNavigationBean.class).editProjects(closeJs);
    }

    public void cmdCreateProject_Click(String closeJs) {
        JSFUtils.getManagedBean(ManagedBeanNames.navigation_projectBean,
                ProjectNavigationBean.class).createProject(closeJs);
    }

	public long getUsersCount() {
		return usersCount;
	}

	public String getViewIdpUsersLabel() {
		long idpUsersCount = IdpFacade.getIdpUsersCount();
		return idpUsersCount > 0 ? "View (" + String.valueOf(idpUsersCount)
				+ ")" : "View";
	}

	public void cmdViewIdpUsers_Click(String closeJs) {
		JSFUtils.getManagedBean(ManagedBeanNames.navigation_idpBean,
				IdpNavigationBean.class).viewIdpUsers(closeJs);
	}

	public void cmdCreateIdpUser_Click(String closeJs) {
		JSFUtils.getManagedBean(ManagedBeanNames.navigation_idpBean,
				IdpNavigationBean.class).addIdpUserDetail(closeJs);
	}

}
