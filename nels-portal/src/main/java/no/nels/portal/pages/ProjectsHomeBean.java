package no.nels.portal.pages;

import no.nels.client.UserApi;
import no.nels.commons.abstracts.ANumberId;
import no.nels.commons.abstracts.AProjectMembership;
import no.nels.commons.model.NumberIndexedList;
import no.nels.commons.model.ProjectUser;
import no.nels.commons.model.projectmemberships.NormalUserProjectMembership;
import no.nels.commons.model.projectmemberships.PIProjectMembership;
import no.nels.commons.utilities.StringUtilities;
import no.nels.portal.abstracts.ASecureBean;
import no.nels.portal.Brokers.ProjectBroker;
import no.nels.portal.facades.SecurityFacade;
import no.nels.portal.facades.SessionFacade;
import no.nels.portal.facades.URLParametersFacade;
import no.nels.portal.model.ProjectUserGridModel;
import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.model.enumerations.SessionItemKeys;
import no.nels.portal.model.enumerations.TabView;
import no.nels.portal.session.ContentNavigatorBean;
import no.nels.portal.utilities.JSFUtils;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.util.HashMap;

@ManagedBean(name = ManagedBeanNames.pages_projects_home)
@ViewScoped
public class ProjectsHomeBean extends ASecureBean {

    private ProjectUserGridModel userProjects;

    private ProjectUser[] selectedProjects;

    private String blockHeader = "Projects";


    public ProjectUserGridModel getUserProjects() {
        return userProjects;
    }

    public ProjectUser[] getSelectedProjects() {
        return selectedProjects;
    }

    public void setSelectedProjects(ProjectUser[] selectedProjects) {
        this.selectedProjects = selectedProjects;
    }

    private void setCurrentFolder(String currentFolder) {
        JSFUtils.getManagedBean(ManagedBeanNames.session_contentNavigatorBean, ContentNavigatorBean.class).setCurrentFolder(currentFolder, TabView.NELS);
    }

    private String getCurrentFolder() {
        return JSFUtils.getManagedBean(ManagedBeanNames.session_contentNavigatorBean, ContentNavigatorBean.class).getCurrentFolder();
    }


    public String getBlockHeader() {
        return blockHeader;
    }

    public String[] getNavStack() {

        String[] pieces = this.getCurrentFolder().split("/");
        String[] ret = new String[pieces.length];
        ret[0] = pieces[0];
        for (int i = 1; i < pieces.length; i++) {
            ret[i] = ret[i - 1] + "/" + pieces[i];
        }
        return ret;
    }

    public String getPathNavigationString(String path) {
        return StringUtilities.EncryptSimple(path, no.nels.portal.Config.getEncryptionSalt());
    }

    public String getLastPath(String path) {
        return path.contains("/") ? org.apache.commons.lang.StringUtils
                .substringAfterLast(path, "/") : path;
    }



    public String getPathForProject(String projectName) {
        return "Projects/" + projectName;
    }

    @Override
    public String getPageTitle() {
        SessionFacade.setSessionObject(SessionItemKeys.LAST_JOB_FETCH_TIME, new Long(0));
        if (!isPostback()) {
            secure();
            String path = URLParametersFacade.getMustUrLParameter("path");
            long userId = SecurityFacade.getUserBeingViewed().getId();
            path = StringUtilities.DecryptSimple(path, no.nels.portal.Config.getEncryptionSalt());
            if (!path.equalsIgnoreCase("") && path.startsWith("Projects")) {
                setCurrentFolder(path);
                if (path.equals("Projects")) {
                    NumberIndexedList projectsOfUser = UserApi.getProjectsForUser(userId);
                    this.userProjects = new ProjectUserGridModel(
                            projectsOfUser);
                    //put the project indexed list into a session
                    SessionFacade.setSessionObject(SessionItemKeys.PROJECTS_OF_USER, userId, constructProjectNameIndexedHash(projectsOfUser));
                }
            }
            this.registerRequestUrl();
        }

        return "NeLS User Home";
    }

    private HashMap<String, ProjectUser> constructProjectNameIndexedHash(NumberIndexedList projects) {
        HashMap<String, ProjectUser> ret = new HashMap<>();
        for (ANumberId aNumberId : projects) {
            ProjectUser p = (ProjectUser) aNumberId;
            ret.put(ProjectBroker.getStorageRoot(p.getProject()), p);
        }
        return ret;
    }

    @Override
    public void secure() {

    }

    public String getRoleFeatures(AProjectMembership membership) {
        return membership.equals(new NormalUserProjectMembership()) ? "Add files & folders, Navigate & Download" : "Manage File system: Add, Rename, Navigate,Download, Delete all Content";
    }

    public boolean canSeeDetails(AProjectMembership membership) {
        return membership.equals(new PIProjectMembership());
    }
}
