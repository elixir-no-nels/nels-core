package no.nels.portal.pages.users;

import no.nels.client.ProjectApi;
import no.nels.client.UserApi;
import no.nels.commons.model.NelsProject;
import no.nels.commons.model.NelsUser;
import no.nels.commons.model.ProjectUser;
import no.nels.commons.model.projectmemberships.NormalUserProjectMembership;
import no.nels.commons.model.projectmemberships.PIProjectMembership;
import no.nels.commons.model.projectmemberships.PowerUserProjectMembership;
import no.nels.portal.abstracts.ASecureBean;
import no.nels.portal.facades.*;
import no.nels.portal.model.ProjectUserGridModel;
import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.model.enumerations.SessionItemKeys;
import no.nels.portal.navigation.ProjectNavigationBean;
import no.nels.portal.utilities.Constants;
import no.nels.portal.utilities.JSFUtils;
import no.nels.portal.utilities.ProjectMembershipTypeUtilities;
import no.nels.portal.utilities.UserTypeUtilities;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;
import java.util.ArrayList;
import java.util.List;

@ManagedBean(name = ManagedBeanNames.pages_users_userdetail_view)
@ViewScoped
public class UserDetailViewBean extends ASecureBean {
	private NelsUser user;
	private String storageUsername;
	private ProjectUserGridModel projects;
	private ProjectUser[] selectedProjects;
	private String pickForGeneral = "pick-general";
	private List<SelectItem> projectMembershipTypeList = new ArrayList<SelectItem>() {
		{
			this.add(new SelectItem(new PIProjectMembership().getId(),
					Constants.PI));
			this.add(new SelectItem(new PowerUserProjectMembership().getId(),
					Constants.POWERUSER));
			this.add(new SelectItem(new NormalUserProjectMembership().getId(),
					Constants.PROJECTNORMALUSER));
		}
	};

	public ProjectUserGridModel getProjects() {
		return projects;
	}

	public List<SelectItem> getProjectMembershipTypeList() {
		return projectMembershipTypeList;
	}

	public String getStorageUsername() {
		return storageUsername;
	}

	public NelsUser getUser() {
		return user;
	}

	public ProjectUser[] getSelectedProjects() {
		return selectedProjects;
	}

	public void setSelectedProjects(ProjectUser[] selectedProjects) {
		this.selectedProjects = selectedProjects;
	}

	public long getSelectedProjectMembershipType() {
		if (!SessionFacade
				.isSessionObjectSet(SessionItemKeys.PROJECT_MEMBERSHIP_TYPE_IN_USER_DETAIL)) {
			SessionFacade.setSessionObject(SessionItemKeys.PROJECT_MEMBERSHIP_TYPE_IN_USER_DETAIL,
					new NormalUserProjectMembership().getId());
		}
		return (Long) SessionFacade
				.getSessionObject(SessionItemKeys.PROJECT_MEMBERSHIP_TYPE_IN_USER_DETAIL);
	}

	public void setSelectedProjectMembershipType(
			long selectedProjectMembershipType) {
		SessionFacade.setSessionObject(SessionItemKeys.PROJECT_MEMBERSHIP_TYPE_IN_USER_DETAIL,
				selectedProjectMembershipType);
	}

	public String getProjectMembershipTypeName(long membershipId) {
		return ProjectMembershipTypeUtilities
				.getProjectMembershipTypeName(no.nels.commons.utilities.ProjectMembershipTypeUtilities
						.getProjectMembership(membershipId));
	}

	public String getSystemUserTypeName(long typeId) {
		return UserTypeUtilities.getUserTypeName(typeId);
	}

	public void assignToNewProject(String closeJs) {
		JSFUtils.getManagedBean(ManagedBeanNames.navigation_projectBean,
				ProjectNavigationBean.class).pickProjects(this, this.pickForGeneral, closeJs, false);
	}

	public void removeUserFromProject() {
		if (this.selectedProjects != null && this.selectedProjects.length != 0) {
			List<Long> projectIdsList = new ArrayList<Long>(
					this.selectedProjects.length);
			for (ProjectUser projectUser : this.selectedProjects) {
				projectIdsList.add(projectUser.getProject().getId());
			}
			if (ProjectApi.removeUserFromProject(new ArrayList<Long>() {{this.add(user.getId());}},
					projectIdsList)) {
				MessageFacade.addInfo("The user is removed from the selected project(s) successfully", "");
				this.selectedProjects = null;
				this.reloadProjects();
			} else {
				MessageFacade.AddError("The user is removed from the selected project(s) unsuccessfully",
						"Internal error");
			}
		} else {
			MessageFacade.noRowsSelected();
		}
	}

	public void modifyUserMembershipType() {
		if (this.selectedProjects != null && this.selectedProjects.length != 0) {
			List<Long> projectIdsList = new ArrayList<Long>(
					this.selectedProjects.length);
			for (ProjectUser projectUser : this.selectedProjects) {
				projectIdsList.add(projectUser.getProject().getId());
			}
			if (ProjectApi
					.changeProjectMembership(new ArrayList<Long>() {{this.add(user.getId());}},
							projectIdsList,
							no.nels.commons.utilities.ProjectMembershipTypeUtilities
									.getProjectMembership(this.getSelectedProjectMembershipType()))) {
				MessageFacade.addInfo("The membership is modified successfully",
						"");
				this.selectedProjects = null;
				this.reloadProjects();
			} else {
				MessageFacade.AddError(
						"The membership is modified unsuccessfully",
						"Internal error");
			}
		} else {
			MessageFacade.noRowsSelected();
		}
	}

	public void secure() {

		// TODO: this method needs some security implementations

	}

	public String getPageTitle() {
		if (!isPostback()) {
			secure();
			this.registerRequestUrl();

			long nelsId = Long.valueOf(this.getUrlParameter(
					no.nels.portal.model.enumerations.URLParameterNames.ID)
					.toString());
			this.user = UserApi.getNelsUserById(nelsId);
			if (this.user == null) {
				NavigationFacade.showInvalidOperation();
			}
			this.checkPickerReturn();
			this.projects = new ProjectUserGridModel(
					UserApi.getProjectsForUser(this.user.getId()));
			this.storageUsername = no.nels.client.Admin
					.getStorageUsername(SecurityFacade.getLoggedInUser()
							.getId(), nelsId);
		}

		return "User details";
	}

	public void reloadProjects() {
		this.projects = new ProjectUserGridModel(
				UserApi.getProjectsForUser(this.user.getId()));
	}

	public void checkPickerReturn() {
		if (PickerFacade.isReturnedFromPicker(this.pickForGeneral)) {
			Object[] selectedValues = PickerFacade
					.getPickerReturnedValues(this.pickForGeneral);

			List<Long> projectIds = new ArrayList<Long>(selectedValues.length);
			for (Object object : selectedValues) {
				projectIds.add(((NelsProject) object).getId());
			}

			//remove the repeated projects from the PICKER
			List<Integer> projectIdListForUser = ProjectApi.getPojectIdListForUser(this.user.getId());
			for (long existingId : projectIdListForUser) {
				if (projectIds.contains(existingId)) {
					projectIds.remove(existingId);
					if (projectIds.size() == 0) {
						break;
					}
				}
			}

			if (projectIds.size() == 0 || ProjectApi
					.addUserToProject(new ArrayList<Long>() {{this.add(user.getId());}},
							projectIds,
							no.nels.commons.utilities.ProjectMembershipTypeUtilities
									.getProjectMembership(this.getSelectedProjectMembershipType()))) {
				if (projectIds.size() == selectedValues.length) {
					MessageFacade
							.addInfo(
									"The user is added into the selected project(s) with selected membership successfully");
				} else if (projectIds.size() == 0) {
					MessageFacade
							.addInfo(
									"The user has already existed in the selected project(s), if you want to change the membership type, please use the Modify Membership button");
				} else {
					MessageFacade.addInfo(
							"The user is added into " + projectIds.size() + " of projects you selected, and the user has already existed in " + (selectedValues.length - projectIds.size()) + " of the projects before. If you want to change the membership type, please use the Modify Membership button");
				}

			} else {
				MessageFacade
						.AddError(
								"The user is added into the selected project(s) with selected membership unsuccessfully",
								"");
			}
		}
	}

}
