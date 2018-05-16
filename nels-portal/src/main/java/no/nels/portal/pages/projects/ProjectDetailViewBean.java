package no.nels.portal.pages.projects;


import no.nels.client.ProjectApi;
import no.nels.commons.abstracts.AProjectMembership;
import no.nels.commons.model.NelsProject;
import no.nels.commons.model.NelsUser;
import no.nels.commons.model.ProjectUser;
import no.nels.commons.model.projectmemberships.NormalUserProjectMembership;
import no.nels.commons.model.projectmemberships.PIProjectMembership;
import no.nels.commons.model.projectmemberships.PowerUserProjectMembership;
import no.nels.commons.utilities.ProjectMembershipTypeUtilities;
import no.nels.portal.abstracts.ASecureBean;
import no.nels.portal.facades.*;
import no.nels.portal.model.ProjectUserGridModel;
import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.model.enumerations.SessionItemKeys;
import no.nels.portal.model.enumerations.URLParameterNames;
import no.nels.portal.navigation.ProjectNavigationBean;
import no.nels.portal.navigation.UserNavigationBean;
import no.nels.portal.utilities.Constants;
import no.nels.portal.utilities.JSFUtils;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;
import java.util.ArrayList;
import java.util.List;

@ManagedBean(name = ManagedBeanNames.pages_projects_projectdetail_view)
@ViewScoped
public class ProjectDetailViewBean extends ASecureBean {
    private NelsProject project;
    private ProjectUserGridModel users;
    private ProjectUser[] selectedUsers;
    private String pickForGeneral = "pick-general";

    private List<SelectItem> projectMembershipTypeList = new ArrayList<SelectItem>() {
        {
            this.add(new SelectItem(new NormalUserProjectMembership().getId(),
                    Constants.PROJECTNORMALUSER));
            this.add(new SelectItem(new PowerUserProjectMembership().getId(),
                    Constants.POWERUSER));
            this.add(new SelectItem(new PIProjectMembership().getId(),
                    Constants.PI));
        }
    };

    public ProjectUserGridModel getUsers() {
        return users;
    }

    public List<SelectItem> getProjectMembershipTypeList() {
        return projectMembershipTypeList;
    }

    public NelsProject getProject() {
        return project;
    }

    public ProjectUser[] getSelectedUsers() {
        return selectedUsers;
    }

    public void setSelectedUsers(ProjectUser[] selectedUsers) {
        this.selectedUsers = selectedUsers;
    }

    public long getSelectedProjectMembershipType() {
        if (!SessionFacade
                .isSessionObjectSet(SessionItemKeys.PROJECT_MEMBERSHIP_TYPE_IN_PROJECT_DETAIL)) {
            SessionFacade.setSessionObject(SessionItemKeys.PROJECT_MEMBERSHIP_TYPE_IN_PROJECT_DETAIL,
                    new NormalUserProjectMembership().getId());
        }
        return (Long) SessionFacade
                .getSessionObject(SessionItemKeys.PROJECT_MEMBERSHIP_TYPE_IN_PROJECT_DETAIL);
    }

    public void setSelectedProjectMembershipType(
            long selectedProjectMembershipType) {
        SessionFacade.setSessionObject(SessionItemKeys.PROJECT_MEMBERSHIP_TYPE_IN_PROJECT_DETAIL,
                selectedProjectMembershipType);
    }

    public String getProjectMembershipTypeName(long membershipId) {
        return no.nels.portal.utilities.ProjectMembershipTypeUtilities
                .getProjectMembershipTypeName(ProjectMembershipTypeUtilities
                        .getProjectMembership(membershipId));
    }

    @Override
    public String getPageTitle() {
        if (!isPostback()) {

            this.registerRequestUrl();

            long projectId = Long.valueOf(this.getUrlParameter(
                    URLParameterNames.ID).toString());
            this.project = ProjectApi.getProject(projectId);

            if (this.project == null) {
                NavigationFacade.showInvalidOperation();
            }
            secure();

            try {
                this.checkPickerReturn();
            } catch (Exception e) {
                LoggingFacade.logDebugInfo(e);
            }
            this.users = new ProjectUserGridModel(
                    ProjectApi.getMembersInProject(this.project.getId()));
        }

        return "Project Details";
    }

    @Override
    public void secure() {
        if (!SecurityFacade.isUserAdmin() && !SecurityFacade.isUserHelpDesk()) {
            AProjectMembership membership = ProjectApi.getProjectMembershipType(this.project.getId(), SecurityFacade.getLoggedInUser().getId());
            if (membership == null) {
                NavigationFacade.showAccessDenied();
            } else if (!membership.equals(new PIProjectMembership())) {
                NavigationFacade.showAccessDenied();
            }
        }
    }

    public void reloadUsers() {
        this.users = new ProjectUserGridModel(
                ProjectApi.getMembersInProject(this.project.getId()));
    }

    public void cmdEditProject_Click(String closeJs) {
        JSFUtils.getManagedBean(ManagedBeanNames.navigation_projectBean,
                ProjectNavigationBean.class).editProject(closeJs,
                Long.toString(this.project.getId()));
    }

    public void addUserToProject(String closeJs) {
        JSFUtils.getManagedBean(ManagedBeanNames.navigation_userBean,
                UserNavigationBean.class).pickUsers(this, this.pickForGeneral,
                closeJs, false);

    }

    public void removeUserFromProject() throws Exception {
        if (this.selectedUsers != null && this.selectedUsers.length != 0) {
            List<Long> userIdsList = new ArrayList<Long>(
                    this.selectedUsers.length);
            for (ProjectUser projectUser : this.selectedUsers) {
                userIdsList.add(projectUser.getUser().getId());
            }
            try {
                Boolean isRemoved = ProjectApi.removeUserFromProject(userIdsList,
                        new ArrayList<Long>() {{
                            this.add(project.getId());
                        }});
                if (isRemoved) {
                    MessageFacade.addInfo("The user(s) are removed from this project successfully", "");
                    this.selectedUsers = null;
                    this.reloadUsers();
                } else {
                    MessageFacade.AddError("The user(s) are removed from this project unsuccessfully",
                            "Internal error");
                }
            } catch (RuntimeException e) {
                LoggingFacade.logDebugInfo(e);
                MessageFacade.AddError("The user(s) are removed from this project unsuccessfully",
                        "Internal error");
            }
        } else {
            MessageFacade.noRowsSelected();
        }
    }

    public void modifyUserMembershipType() throws Exception {
        if (this.selectedUsers != null && this.selectedUsers.length != 0) {
            List<Long> userIdsList = new ArrayList<Long>(
                    this.selectedUsers.length);
            AProjectMembership projectMembership = ProjectMembershipTypeUtilities.getProjectMembership(this.getSelectedProjectMembershipType());
            long membershipId = projectMembership.getId();
            String projectRole = membershipId == 3 ? "member" : membershipId == 2 ? "poweruser" : "admin";
            for (ProjectUser projectUser : this.selectedUsers) {
                userIdsList.add(projectUser.getUser().getId());
                LoggingFacade.logDebugInfo("changeProjectMembership:" + SecurityFacade.getLoggedInUser().getId() + "," + this.project.getId() + "," + projectUser.getUser().getId() + "," + projectRole);
            }
            try {
                Boolean isChanged = ProjectApi.changeProjectMembership(userIdsList,
                        new ArrayList<Long>() {{
                            this.add(project.getId());
                        }},
                        projectMembership);
                if (isChanged) {
                    MessageFacade.addInfo("The membership is modified successfully",
                            "");
                    this.selectedUsers = null;
                    this.reloadUsers();
                } else {
                    MessageFacade.AddError(
                            "The membership is modified unsuccessfully",
                            "Internal error");
                }
            } catch (RuntimeException e) {
                LoggingFacade.logDebugInfo(e);
                MessageFacade.AddError("The membership is modified unsuccessfully", "Internal error");
            }
        } else {
            MessageFacade.noRowsSelected();
        }
    }

    public void checkPickerReturn() throws Exception {

        if (PickerFacade.isReturnedFromPicker(this.pickForGeneral)) {
            NelsUser[] selectedUsers = (NelsUser[]) PickerFacade
                    .getPickerReturnedValues(this.pickForGeneral);
            // change to list
            List<Long> userIds = new ArrayList<Long>(selectedUsers.length);
            for (NelsUser usr : selectedUsers) {
                userIds.add(usr.getId());
            }

            long projectId = this.project.getId();

            //remove the repeated users from the PICKER
            List<Integer> userIdListForProject = ProjectApi.getUserIdListForProject(projectId);
            for (long existingId : userIdListForProject) {
                if (userIds.contains(existingId)) {
                    userIds.remove(existingId);
                    if (userIds.size() == 0) {
                        break;
                    }
                }
            }

            ArrayList<Long> projectIds = new ArrayList<>();
            projectIds.add(project.getId());
            AProjectMembership membership = ProjectMembershipTypeUtilities.getProjectMembership(this.getSelectedProjectMembershipType());

            try {
                if (userIds.size() == 0 || ProjectApi.addUserToProject(userIds, projectIds, membership)) {
                    if (userIds.size() == selectedUsers.length) {
                        //all selectedUsers are new members of the project
                        MessageFacade.addInfo(
                                "The user(s) are added into the project with selected membership successfully");

                    } else if (userIds.size() == 0) {
                        MessageFacade.addInfo(
                                "The user(s) you selected have already existed in the project before, if you want to change the membership type, please use the Modify Membership button");
                    } else {
                        //some users are new members
                        MessageFacade.addInfo(userIds.size() + " of users you selected are added into the project, and " + (selectedUsers.length - userIds.size()) + " users have already existed in the project before. If you want to change the membership type, please use the Modify Membership button");
                    }

                } else {
                    MessageFacade
                            .AddError(
                                    "The user(s) are added into the selected project with selected membership unsuccessfully",
                                    "");
                }
            } catch (RuntimeException e) {
                LoggingFacade.logDebugInfo(e);
                MessageFacade
                        .AddError(
                                "The user(s) are added into the selected project with selected membership unsuccessfully",
                                "Internal error");
            }
        }
    }

}
