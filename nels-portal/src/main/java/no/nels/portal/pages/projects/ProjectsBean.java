package no.nels.portal.pages.projects;

import no.nels.client.ProjectApi;
import no.nels.commons.abstracts.ASystemUser;
import no.nels.commons.model.NelsProject;
import no.nels.commons.model.systemusers.AdministratorUser;
import no.nels.commons.model.systemusers.HelpDeskUser;
import no.nels.portal.abstracts.ASecureBean;
import no.nels.portal.facades.*;
import no.nels.portal.model.ProjectGridModel;
import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.model.enumerations.PageModes;
import no.nels.portal.model.enumerations.URLParameterNames;
import no.nels.portal.navigation.ProjectNavigationBean;
import no.nels.portal.utilities.JSFUtils;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.util.ArrayList;
import java.util.List;

@ManagedBean(name = ManagedBeanNames.pages_projects_projects)
@ViewScoped
public class ProjectsBean extends ASecureBean {
	private ProjectGridModel projects = new ProjectGridModel(
			ProjectApi.getAllProjects());
	private NelsProject[] selectedProjects;
	private NelsProject selectedProject;

	private String searchId;
	private String searchName;

	public String getSearchId() {
		return searchId;
	}

	public void setSearchId(String searchId) {
		this.searchId = searchId;
	}

	public String getSearchName() {
		return searchName;
	}

	public void setSearchName(String searchName) {
		this.searchName = searchName;
	}

	public ProjectGridModel getProjects() {
		return projects;
	}

	public NelsProject[] getSelectedProjects() {
		return selectedProjects;
	}

	public void setSelectedProjects(NelsProject[] selectedProjects) {
		this.selectedProjects = selectedProjects;
	}

	public NelsProject getSelectedProject() {
		return selectedProject;
	}

	public void setSelectedProject(NelsProject selectedProject) {
		this.selectedProject = selectedProject;
	}

	@Override
	public String getPageTitle() {
		if (!isPostback()) {
			secure();
			this.registerRequestUrl();
			if (this.getPageMode().equalsIgnoreCase(PageModes.PickOne)
					|| this.getPageMode().equalsIgnoreCase(
							PageModes.PickMultiple)) {
				URLParametersFacade
						.requireURLParameter(URLParameterNames.PickerCallKey);
			}
		}
		return "Projects";
	}

	@Override
	public void secure() {
		ArrayList<ASystemUser> userTypes = new ArrayList<ASystemUser>() {
			{
				add(new AdministratorUser());
				add(new HelpDeskUser());
			}
		};
		SecurityFacade.requireSystemUserType(userTypes);
	}

	public void reloadProjects() {

		long id = -1;
		try {
			id = Long.parseLong(this.searchId);
		}
		catch (Exception ex){searchId="";}

		this.projects = new ProjectGridModel(ProjectApi.searchProjects(id,this.searchName.trim()));
		//this.projects = new ProjectGridModel(ProjectFacade.getAllProjects());

	}

	public void viewProject(long projectId) {
		JSFUtils.getManagedBean(ManagedBeanNames.navigation_projectBean,
				ProjectNavigationBean.class).viewProjectDetail(projectId);
	}

	public void deleteProjects() {
		if (this.selectedProjects != null && this.selectedProjects.length != 0) {
			List<Long> projectIdsList = new ArrayList<Long>(
					this.selectedProjects.length);
			for (NelsProject project : this.selectedProjects) {
				projectIdsList.add(project.getId());
			}

			if (ProjectApi.deleteProject(projectIdsList)) {
				MessageFacade.addInfo(
						"You have removed the project(s) successfully", "");
				this.selectedProjects = null;
				this.reloadProjects();
			} else {
				MessageFacade.AddError(
						"You have not removed the project(s) successfully",
						"Internal error");
			}
		} else {
			MessageFacade.noRowsSelected();
		}
	}

	public void cmdAcceptPicker_Click() {
		if (this.getPageMode().equalsIgnoreCase(PageModes.PickMultiple)) {
			if (this.selectedProjects.length > 0) {
				PickerFacade.returnFromPicker(this, this.selectedProjects);
			} else {
				MessageFacade.noRowsSelected();
			}
		} else if (this.getPageMode().equalsIgnoreCase(PageModes.PickOne)) {
			if (this.selectedProject != null) {
				PickerFacade.returnFromPicker(this,
						new Object[] { this.selectedProject });
			} else {
				MessageFacade.noRowsSelected();
			}
		}
	}

	public void cmdCancelPicker_Click() {
		NavigationFacade.closePopup();
	}


	public void cmdSearch_Click(){
		reloadProjects();
	}

	public void cmdRefreshSearch_Click(){
		this.searchId = "";
		this.searchName="";
		reloadProjects();
	}

}
