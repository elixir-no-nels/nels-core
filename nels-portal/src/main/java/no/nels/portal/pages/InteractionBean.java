package no.nels.portal.pages;

import no.nels.client.UserApi;
import no.nels.client.model.FileFolder;
import no.nels.commons.abstracts.ANumberId;
import no.nels.commons.abstracts.AStringId;
import no.nels.commons.model.NumberIndexedList;
import no.nels.commons.model.ProjectUser;
import no.nels.commons.model.StringIndexedList;
import no.nels.commons.utilities.StringUtilities;
import no.nels.portal.Brokers.ProjectBroker;
import no.nels.portal.facades.LoggingFacade;
import no.nels.portal.facades.MessageFacade;
import no.nels.portal.facades.SecurityFacade;
import no.nels.portal.facades.SessionFacade;
import no.nels.portal.model.FileFolderGridModel;
import no.nels.portal.model.HomeViewItemModel;
import no.nels.portal.model.ProjectUserGridModel;
import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.model.enumerations.SessionItemKeys;
import no.nels.portal.model.enumerations.TabView;
import no.nels.portal.session.ContentNavigatorBean;
import no.nels.portal.utilities.JSFUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Kidane on 06.01.2016.
 */

public abstract class InteractionBean extends FileBean {

    private ProjectUserGridModel userProjectModel;
    private ProjectUser selectedProject;
    private String currentNelsPath;
    private List<HomeViewItemModel> homeViewItems;

    public void init(String currentNelsPath, TabView view) {
        this.currentNelsPath = currentNelsPath;
        setCurrentFolder(currentNelsPath, view);
        loadNelsGridModel(currentNelsPath);
    }

    public abstract String getTargetPage();

    public abstract Map<String, String> getUrlParameters();

    public abstract void transferToOther();

    public abstract boolean transferButtonVisible();

    public abstract String getDestinationForView();

    protected void loadNelsGridModel(String path) {
        /*
        1. home view----display Personal and Projects
        2. project home view ----display user's projects
        3. Personal fileFolder view ----display file folders in Personal or Projects fileFolder view ----display file folders in Projects
        */

        if (isHomeView()) {
            this.homeViewItems = new ArrayList<>(2);
            HomeViewItemModel personal = new HomeViewItemModel("Personal");
            HomeViewItemModel projects = new HomeViewItemModel("Projects");
            this.homeViewItems.add(personal);
            this.homeViewItems.add(projects);

        } else if (isFileFolderView()) {
            try {
                List<FileFolder> items = UserApi.listItems(SecurityFacade.getLoggedInUser().getId(),
                        SecurityFacade.getUserBeingViewed().getId(), path);
                StringIndexedList flFldrs = new StringIndexedList();
                items.stream().forEach(flFldrs::add);
                super.setFileFolders( new FileFolderGridModel(flFldrs));
            } catch (Exception ex) {
                MessageFacade.AddError("Error", ex.getMessage());
            }
        } else if (isProjectsHomeView()) {
            NumberIndexedList projectsOfUser = UserApi.getProjectsForUser(SecurityFacade.getUserBeingViewed().getId());
            SessionFacade.setSessionObject(SessionItemKeys.PROJECTS_OF_USER, SecurityFacade.getUserBeingViewed().getId(), constructProjectNameIndexedHash(projectsOfUser));
            this.userProjectModel = new ProjectUserGridModel(projectsOfUser);
        }


    }

    public boolean isNelsTransferPathValid() {
        boolean isHomeView = isHomeView();
        boolean isProjectsHomeView = isProjectsHomeView();
        return (!isHomeView && !isProjectsHomeView);
    }



//    public String[] getNelsNavStack() {
//
//        String[] pieces = getCurrentNelsPath().split("/");
//        String[] ret = new String[pieces.length];
//        ret[0] = pieces[0];
//        for (int i = 1; i < pieces.length; i++) {
//            ret[i] = ret[i - 1] + "/" + pieces[i];
//        }
//
//        //LoggingFacade.logDebugInfo("nels path:" + getCurrentNelsPath() + ",ret: " + Arrays.toString(ret));
//        return ret;
//    }


    private HashMap<String, ProjectUser> constructProjectNameIndexedHash(NumberIndexedList projects) {
        HashMap<String, ProjectUser> ret = new HashMap<>();
        for (ANumberId aNumberId : projects) {
            ProjectUser p = (ProjectUser) aNumberId;
            ret.put(ProjectBroker.getStorageRoot(p.getProject()), p);
        }
        return ret;
    }

    public boolean isItemExist(String name, boolean isFolder, boolean flag) {
        boolean exit = false;
        String realName;
        if (flag) { // true: to check if the item with the same name and same type exists
            realName = isFolder ? "fldr-" + name : "fl-" + name;
        } else {    // false: to check if the item with the same name but different type exists
            realName = !isFolder ? "fldr-" + name : "fl-" + name;
        }
        for (AStringId fileFolder : super.getFileFolders()) {
            LoggingFacade.logDebugInfo("fileFolder Id:" + fileFolder.getId() + ", realName:" + realName);
            if (fileFolder.getId().equals(realName)) {
                return true;
            }
        }
        return exit;
    }



    public ProjectUserGridModel getUserProjectModel() {
        return userProjectModel;
    }



    public ProjectUser getSelectedProject() {
        return selectedProject;
    }

    public void setSelectedProject(ProjectUser selectedProject) {
        this.selectedProject = selectedProject;
    }

    public String getCurrentNelsPath() {
        return currentNelsPath;
    }

    public void setCurrentNelsPath(String currentNelsPath) {
        this.currentNelsPath = currentNelsPath;
    }

    public List<HomeViewItemModel> getHomeViewItems() {
        return homeViewItems;
    }

    public boolean isHomeView() {
        return getCurrentNelsPath().isEmpty();
    }

    public boolean isProjectsHomeView() {
        return getCurrentNelsPath().equals("Projects");
    }

    public boolean isFileFolderView() {
        if (getCurrentNelsPath().startsWith("Personal")) {
            return true;
        }
        if (getCurrentNelsPath().startsWith("Projects") && !getCurrentNelsPath().equals("Projects")) {
            return true;
        }
        return false;
    }

    public String encrypt(String s) {
        return StringUtilities.EncryptSimple(s, no.nels.portal.Config.getEncryptionSalt());
    }



    public void setCurrentFolder(String currentFolder, TabView view) {
        JSFUtils.getManagedBean(ManagedBeanNames.session_contentNavigatorBean, ContentNavigatorBean.class).setCurrentFolder(currentFolder, view);
    }


}
