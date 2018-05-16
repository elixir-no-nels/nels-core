package no.nels.portal.pages.projects;

import no.nels.client.ProjectApi;
import no.nels.commons.abstracts.ASystemUser;
import no.nels.commons.model.NelsProject;
import no.nels.commons.model.systemusers.AdministratorUser;
import no.nels.commons.model.systemusers.HelpDeskUser;
import no.nels.commons.utilities.StringUtilities;
import no.nels.portal.abstracts.ASecureBean;
import no.nels.portal.facades.MessageFacade;
import no.nels.portal.facades.NavigationFacade;
import no.nels.portal.facades.SecurityFacade;
import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.model.enumerations.PageModes;
import no.nels.portal.model.enumerations.URLParameterNames;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.util.ArrayList;
import java.util.function.Supplier;

@ManagedBean(name = ManagedBeanNames.pages_projects_project)
@ViewScoped
public class ProjectBean extends ASecureBean {
    private NelsProject project;
    private String name;
    private String description;

    @Override
    public String getPageTitle() {
        if (!isPostback()) {
            secure();
            this.registerRequestUrl();
            if (this.getPageMode().equalsIgnoreCase(PageModes.Edit)) {
                this.project = ProjectApi.getProject(Long
                        .valueOf((String) this
                                .getUrlParameter(URLParameterNames.ID)));
                this.name = this.project.getName();
                this.description = this.project.getDescription();
            }
        }
        return "Project";
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

    public void cmdSaveProject_Click() {
        if (this.validateInput()) {
            if (ProjectApi.isProjectNameExisting(this.name.trim())) {
                MessageFacade
                        .AddError("The name is already used, you have to user another project name");
            } else {
                Boolean isCreated = ProjectApi.addNewProject(this.name.trim(),
                        this.description.trim());
                if (isCreated) {
                    MessageFacade.addInfo(
                            "The new project is created successfully", "");
                } else {
                    NavigationFacade.closePopup();
                    MessageFacade.AddError(
                            "Unable to create project",
                            "internal error");
                }
            }
        }
    }

    public void cmdUpdateProject_Click() {
        if (this.validateInput()) {
            if (this.name.trim().equalsIgnoreCase(this.project.getName())
                    && this.description.trim().equalsIgnoreCase(
                    this.project.getDescription())) {
                MessageFacade.addInfo("The project is updated successfully", "");
            } else {
                if (!this.name.trim().equalsIgnoreCase(this.project.getName())) {
                    if (ProjectApi.isProjectNameExisting(this.name.trim())) {
                        MessageFacade
                                .AddError("The name has already existed, you have to user another project name");
                    } else {
                        invokeUpdateProjectMethod(() -> ProjectApi.updateProject(this.project.getId(), this.name, this.description));
                    }
                } else {
                    invokeUpdateProjectMethod(() -> ProjectApi.updateProject(this.project.getId(), this.description));
                }
            }
        }
    }

    private void invokeUpdateProjectMethod(Supplier<Boolean> supplier) {
        if (supplier.get()) {
            MessageFacade.addInfo("The project is updated successfully", "");
        } else {
            MessageFacade.AddError("The project is updated unsuccessfully",
                    "internal error");
        }
    }

    public boolean validateInput() {
        //validate project name
        if (this.name.trim().equalsIgnoreCase("")) {
            MessageFacade.AddError("You must input the project name");
            return false;
        }
        if (this.name.trim().contains(" ") || this.name.trim().contains(".")) {
            MessageFacade.invalidInput("Invalid character in folder name. Space and dot is not allowed in the name. Modify your input and try again");
            return false;
        }
        if (!StringUtilities.isValidFileFolderName(this.name.trim())) {
            MessageFacade.invalidInput("Invalid character in folder name. Modify your input and try again");
            return false;
        }
        //validate description
        if (this.description.trim().equalsIgnoreCase("")) {
            MessageFacade.AddError("You must input the description about the project");
            return false;
        }
        return true;
    }

    public void cmdCancel_Click() {
        NavigationFacade.closePopup();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public NelsProject getProject() {
        return project;
    }
}
