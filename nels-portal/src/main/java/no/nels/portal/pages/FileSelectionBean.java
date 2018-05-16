package no.nels.portal.pages;


import no.nels.client.UserApi;
import no.nels.client.model.FileFolder;
import no.nels.commons.abstracts.ASystemUser;
import no.nels.commons.model.StringIndexedList;
import no.nels.commons.model.systemusers.AdministratorUser;
import no.nels.commons.model.systemusers.HelpDeskUser;
import no.nels.commons.model.systemusers.NormalUser;
import no.nels.commons.utilities.StringUtilities;
import no.nels.portal.abstracts.ASecureBean;
import no.nels.portal.facades.MessageFacade;
import no.nels.portal.facades.SecurityFacade;
import no.nels.portal.facades.URLParametersFacade;
import no.nels.portal.model.FileFolderGridModel;
import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.model.enumerations.TabView;
import no.nels.portal.model.enumerations.URLParameterNames;
import no.nels.portal.model.sbi.SbiTransferringMode;
import no.nels.portal.session.ContentNavigatorBean;
import no.nels.portal.utilities.FileUtils;
import no.nels.portal.utilities.JSFUtils;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.util.ArrayList;
import java.util.List;

@ManagedBean(name = ManagedBeanNames.pages_file_selection)
@ViewScoped
public class FileSelectionBean extends ASecureBean{
    private FileFolderGridModel fileFolders;
    private String blockHeader = "My Data";
    private String transferringMode;
    private FileFolder[] selectedFileFolders;

    public FileFolder[] getSelectedFileFolders() {
        return selectedFileFolders;
    }

    public void setSelectedFileFolders(FileFolder[] selectedFileFolders) {
        this.selectedFileFolders = selectedFileFolders;
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
        if (!isPostback()) {
            secure();
            String path = URLParametersFacade.getMustUrLParameter(URLParameterNames.PATH);

            path = StringUtilities.DecryptSimple(path,no.nels.portal.Config.getEncryptionSalt());
            if (path.startsWith("Personal")) {
                boolean isFolder = Boolean.parseBoolean(URLParametersFacade
                        .getMustUrLParameter(URLParameterNames.IS_FOLDER));
                this.transferringMode = URLParametersFacade.getMustUrLParameter(URLParameterNames.Mode);
                if (isFolder) {
                    setCurrentFolder(path);
                    loadFolderItems();
                    blockHeader = (SecurityFacade.getLoggedInUser()
                            .equals(SecurityFacade.getUserBeingViewed())) ? "My Data"
                            : SecurityFacade.getUserBeingViewed().getIdpUser()
                            .getFirstname()
                            + " "
                            + SecurityFacade.getUserBeingViewed()
                            .getIdpUser().getLastname()
                            + " (NeLS ID: "
                            + SecurityFacade.getUserBeingViewed()
                            .getId() + ")";
                }
            }
            this.registerRequestUrl();
        }
        return "NeLS User Home";
    }

    public String getTransferringMode() {
        return this.transferringMode;
    }

    public void loadFolderItems() {
        try {
            List<FileFolder> items = UserApi.listItems(SecurityFacade
                    .getLoggedInUser().getId(), SecurityFacade
                    .getUserBeingViewed().getId(), getCurrentFolder());
            StringIndexedList flFldrs = new StringIndexedList();
            if (this.getTransferringMode().equals(SbiTransferringMode.PULL.getValue())) {
                items.stream().filter(FileFolder::isFolder).forEach(flFldrs::add);
            } else {
                items.stream().forEach(flFldrs::add);
            }
            this.fileFolders = new FileFolderGridModel(flFldrs);
        } catch (Exception ex) {
            MessageFacade.AddError("Error", ex.getMessage());
        }
    }

    public FileFolderGridModel getFileFolders() {
        return fileFolders;
    }

    public String getFileSizeForDisplay(long byteSize) {
        return FileUtils.getDisplayString(byteSize);
    }

    public String getLastPath(String path) {
        return path.contains("/") ? org.apache.commons.lang.StringUtils
                .substringAfterLast(path, "/") : path;
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


    private void setCurrentFolder(String currentFolder) {
        JSFUtils.getManagedBean(ManagedBeanNames.session_contentNavigatorBean, ContentNavigatorBean.class).setCurrentFolder(currentFolder, TabView.NELS);
    }

    protected String getCurrentFolder() {
        return JSFUtils.getManagedBean(ManagedBeanNames.session_contentNavigatorBean, ContentNavigatorBean.class).getCurrentFolder();
    }

    public String getPathNavigationString(String path) {
        return StringUtilities.EncryptSimple(path,no.nels.portal.Config.getEncryptionSalt());
    }


}
