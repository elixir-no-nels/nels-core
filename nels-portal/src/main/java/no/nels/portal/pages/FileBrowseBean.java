package no.nels.portal.pages;

import no.nels.client.Config;
import no.nels.client.model.FileFolder;
import no.nels.commons.abstracts.ASystemUser;
import no.nels.commons.model.systemusers.AdministratorUser;
import no.nels.commons.model.systemusers.HelpDeskUser;
import no.nels.commons.model.systemusers.NormalUser;
import no.nels.commons.utilities.StringUtilities;
import no.nels.portal.facades.*;
import no.nels.portal.model.FileFolderSelection;
import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.model.enumerations.SessionItemKeys;
import no.nels.portal.model.enumerations.TabView;
import no.nels.portal.navigation.StaticNavigationBean;
import no.nels.portal.session.ContentNavigatorBean;
import no.nels.portal.utilities.JSFUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@ManagedBean(name = ManagedBeanNames.pages_file_browse)
@ViewScoped
public class FileBrowseBean extends FileBean implements Serializable {



    private String blockHeader = "My Data";
    private boolean sendToGalaxy = false;
    private String galaxyFilesList = "";
    private static final Logger logger = LogManager.getLogger(FileBrowseBean.class);

    public boolean isSendToGalaxy() {
        return sendToGalaxy;
    }

    public boolean isPersonal() {
        return getCurrentFolder().startsWith("Personal");
    }


    public void setCurrentFolder(String currentFolder) {
        JSFUtils.getManagedBean(ManagedBeanNames.session_contentNavigatorBean, ContentNavigatorBean.class).setCurrentFolder(currentFolder, TabView.NELS);
    }

    public boolean isProjectsList() {
        return this.getCurrentFolder().equals("Projects");
    }

    public boolean isProjectFileSystem() {
        return this.getCurrentFolder().contains("Projects") && !isProjectsList();
    }

    public boolean isFileBrowserVisible() {
        return !isProjectsList();
    }

    public boolean isFileFolderButtonsVisible() {
        return !isProjectsList();
    }


    public String getBlockHeader() {
        return blockHeader;
    }

    public void secure() {
        ArrayList<ASystemUser> userTypes = new ArrayList<ASystemUser>() {
            {
                add(new NormalUser());
                add(new AdministratorUser());
                add(new HelpDeskUser());
            }
        };
        SecurityFacade.requireSystemUserType(userTypes);
    }

    public String getPageTitle() {
        MessageFacade.handleDelayedMessages();

        SessionFacade.setSessionObject(SessionItemKeys.LAST_JOB_FETCH_TIME, new Long(0));
        if (!isPostback()) {
            secure();
            this.registerRequestUrl();
            try {
                String path = URLParametersFacade.isUrlParameterSet("path") ? StringUtilities.DecryptSimple(URLParametersFacade.getURLParameter("path"), no.nels.portal.Config.getEncryptionSalt()) : "Personal";
                boolean isFolder = URLParametersFacade.isUrlParameterSet("isFolder") ? Boolean.parseBoolean(URLParametersFacade
                        .getURLParameter("isFolder")) : path.equals("Personal");
                setCurrentFolder(path);
                if (isFolder) {

                    loadFolderItems();
                    if (SecurityFacade.getLoggedInUser().getId() == SecurityFacade.getUserBeingViewed().getId()) { //self view
                        blockHeader = isPersonal() ? "My Data" : "My Projects";
                    } else {
                        blockHeader = SecurityFacade.getUserBeingViewed().getIdpUser().getLastname() + " (NeLS ID: " + SecurityFacade.getUserBeingViewed().getId() + ")";
                    }
                } else {
                    // assume it's a file
                    JSFUtils.streamFileFolderToUser(Config.getFullUrl("user/" + SecurityFacade.getUserBeingViewed().getId() + "/" + path));
                }

                FileFolderSelection selection = getCopyPasteSelection();

                List<FileFolder> selectedItems = new ArrayList<>();

                if (selection != null) {
                    //set selected items
                    FileFolder value;
                    for (String key : selection.getSelectedItems().keySet()) {
                        value = selection.getSelectedItems().get(key);
                        selectedItems.add(value);
                    }
                    logger.debug("selectedItems:" + selectedItems.stream().map(FileFolder::getName).collect(Collectors.joining(", ")));
                    setSelectedFileFolders(selectedItems.toArray(new FileFolder[selectedItems.size()]));
                    LoggingFacade.logDebugInfo("selectedFileFolders:" + Arrays.asList(super.getSelectedFileFolders()).stream().map(FileFolder::getName).collect(Collectors.joining(", ")));
                }
                logger.debug("selected:" + selectedItems.stream().map(FileFolder::getName).collect(Collectors.joining(", ")));


            } catch (Exception ex) {
                MessageFacade.showException(ex);
            }
        }
        return "NeLS User Home";
    }



    public void cmdSendtoGalaxy_Click() {
        if (super.getSelectedFileFolders().length != 0) {
            for (FileFolder ff : super.getSelectedFileFolders()) {
                this.galaxyFilesList = StringUtilities.AppendWithDelimiter(
                        this.galaxyFilesList, ff.getPath(), ",");
            }
            this.sendToGalaxy = true;
        } else {
            MessageFacade.noRowsSelected();
        }

    }

    public void cmdGalaxySave_Click() {
        this.galaxyFilesList = this.getCurrentFolder();
        this.sendToGalaxy = true;
    }

    public String getGalaxyFilesList() {
        return this.galaxyFilesList;
    }


    public void cmdAddFile_Click(String closeJs) {
        JSFUtils.getManagedBean(ManagedBeanNames.navigation_staticBean,
                StaticNavigationBean.class).editFile(this.getCurrentFolder(), true,
                closeJs);
    }














}
