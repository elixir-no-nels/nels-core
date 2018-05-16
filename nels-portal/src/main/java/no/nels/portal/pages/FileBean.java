package no.nels.portal.pages;

import no.nels.client.UserApi;
import no.nels.client.model.FileFolder;
import no.nels.commons.model.ProjectUser;
import no.nels.commons.model.StringIndexedList;
import no.nels.commons.model.projectmemberships.NormalUserProjectMembership;
import no.nels.commons.utilities.StringUtilities;
import no.nels.portal.abstracts.ASecureBean;
import no.nels.portal.facades.MessageFacade;
import no.nels.portal.facades.SecurityFacade;
import no.nels.portal.facades.SessionFacade;
import no.nels.portal.model.FileFolderGridModel;
import no.nels.portal.model.FileFolderSelection;
import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.model.enumerations.SelectionPurpose;
import no.nels.portal.model.enumerations.SessionItemKeys;
import no.nels.portal.navigation.StaticNavigationBean;
import no.nels.portal.session.ContentNavigatorBean;
import no.nels.portal.utilities.FileUtils;
import no.nels.portal.utilities.JSFUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.FileSystems;
import java.util.*;
import java.util.stream.Collectors;

public abstract class FileBean extends ASecureBean implements Serializable {
    private final Logger logger = LogManager.getLogger(getClass());
    private FileFolderGridModel fileFolders;
    private FileFolder[] selectedFileFolders;

    public FileFolderGridModel getFileFolders() {
        return fileFolders;
    }

    public void setFileFolders(FileFolderGridModel fileFolders) {
        this.fileFolders = fileFolders;
    }

    public FileFolder[] getSelectedFileFolders() {
        return selectedFileFolders;
    }

    public void setSelectedFileFolders(FileFolder[] selectedFileFolders) {
        this.selectedFileFolders = selectedFileFolders;
    }

    public boolean isEditDeletePermitted() {
        if (!isPersonal() && isProjectFileSystem()) {//check user's privleges in the project
            long userId = SecurityFacade.getUserBeingViewed().getId();
            if (SessionFacade.isSessionObjectSet(SessionItemKeys.PROJECTS_OF_USER, userId)) {
                HashMap<String, ProjectUser> projectsOfuser = (HashMap<String, ProjectUser>) SessionFacade.getSessionObject(SessionItemKeys.PROJECTS_OF_USER, userId);
                if (projectsOfuser.containsKey(this.getNavStack()[1])) {
                    ProjectUser pu = (ProjectUser) projectsOfuser.get(this.getNavStack()[1]);
                    return !pu.getMembership().equals(new NormalUserProjectMembership()); //normal user can't be allowed to edit, others are ok
                }
                return false;
            }
            return false;
        }
        return true;
    }

    public boolean isPersonal() {
        return getCurrentFolder().startsWith("Personal");
    }

    public String getCurrentFolder() {
        return JSFUtils.getManagedBean(ManagedBeanNames.session_contentNavigatorBean, ContentNavigatorBean.class).getCurrentFolder();
    }

    public boolean isProjectFileSystem() {
        return this.getCurrentFolder().contains("Projects") && !isProjectsList();
    }

    public String[] getNavStack() {

        String[] pieces = this.getCurrentFolder().split("/");
        String[] ret = new String[pieces.length];
        ret[0] = pieces[0];
        for (int i = 1; i < pieces.length; i++) {
            ret[i] = ret[i - 1] + "/" + pieces[i];
        }
        //logger.debug("pieces:" + Arrays.toString(pieces) + ",ret:" + Arrays.toString(ret));
        return ret;
    }

    public String getLastPath(String path) {
        return path.contains("/") ? org.apache.commons.lang.StringUtils
                .substringAfterLast(path, "/") : path;
    }

    public void cmdRename_Click(String oldFilename, String closeJs) {
        JSFUtils.getManagedBean(ManagedBeanNames.navigation_staticBean,
                StaticNavigationBean.class).renameFile(this.getCurrentFolder(),
                oldFilename, closeJs);
    }

    public void cmdAddFolder_Click(String closeJs) {
        JSFUtils.getManagedBean(ManagedBeanNames.navigation_staticBean,
                StaticNavigationBean.class).editFolder(this.getCurrentFolder(), true,
                closeJs);
    }

    public void cmdDelete_Click() {
        if (selectedFileFolders != null && selectedFileFolders.length != 0) {

            boolean allOk = true;

            try {
                List<String> seleteditems = new ArrayList<>(selectedFileFolders.length);
                Arrays.stream(selectedFileFolders).forEach(item -> seleteditems.add(StringUtils.join(new String[]{this.getCurrentFolder(), FileSystems.getDefault().getSeparator(), item.getName()})));
                UserApi.deleteSelectedElements(SecurityFacade
                        .getLoggedInUser().getId(), SecurityFacade
                        .getUserBeingViewed().getId(), seleteditems);
            } catch (Exception e) {
                MessageFacade.AddError(e.getMessage());
                allOk = false;
            }

            if (allOk) {
                MessageFacade.addInfo("Files/folders deleted successfully");
                // clear the copy+cut feature selections since something in the
                // selection could have been deleted
                this.clearCopyPasteFunction();
            }
            this.loadFolderItems();
        } else {
            MessageFacade.noRowsSelected();
        }
    }

    public void handleCutPaste(FileFolderSelection selection) {

        Set<String> src = selection.getSelectedItems().keySet();
        String[] srcArr = src.toArray(new String[src.size()]);
        UserApi.move(SecurityFacade.getLoggedInUser().getId(),
                SecurityFacade.getUserBeingViewed().getId(), srcArr, this.getCurrentFolder());
    }

    public boolean hasCopyPasteSelection() {
        return SessionFacade
                .isSessionObjectSet(SessionItemKeys.FILE_FOLDER_SELECTION);
    }

    public FileFolderSelection getCopyPasteSelection() {
        if (hasCopyPasteSelection()) {
            return (FileFolderSelection) SessionFacade
                    .getSessionObject(SessionItemKeys.FILE_FOLDER_SELECTION);
        }
        return null;
    }

    public boolean isPastePermitted() {
        if (hasCopyPasteSelection()) {
            // validate if possible to paste
            FileFolderSelection selection = getCopyPasteSelection();

            if (selection.isHavingFolder()) {
                FileFolder value;
                for (String key : selection.getSelectedItems().keySet()) {
                    value = selection.getSelectedItems().get(key);
                    if (value.isFolder()) {
                        if (this.getCurrentFolder().equals(selection.getSelectedItemsRootFolder())
                                || this.getCurrentFolder().startsWith(value.getPath())) {
                            return false;
                        } else {
                            return true;
                        }
                    } else {
                        if (this.getCurrentFolder().equals(selection.getSelectedItemsRootFolder())) {
                            return false;
                        } else {
                            return true;
                        }
                    }
                }
            } else {
                return !this.getCurrentFolder().equals(selection.getSelectedItemsRootFolder());
            }
        }
        return false;
    }

    public void cmdCopy_Click() {
        if (selectedFileFolders.length != 0) {
            SessionFacade.setSessionObject(
                    SessionItemKeys.FILE_FOLDER_SELECTION,
                    new FileFolderSelection(this.getCurrentFolder(), selectedFileFolders,
                            SelectionPurpose.COPY));
            List<FileFolder> items = Arrays.asList(selectedFileFolders);
            logger.debug("copy click. selectedFileFolders:" + items.stream().map(FileFolder::getName).collect(Collectors.joining(",")));
        } else {
            MessageFacade.noRowsSelected();
        }
    }

    public void cmdCut_Click() {
        if (selectedFileFolders.length != 0) {
            SessionFacade.setSessionObject(
                    SessionItemKeys.FILE_FOLDER_SELECTION,
                    new FileFolderSelection(this.getCurrentFolder(), selectedFileFolders,
                            SelectionPurpose.CUT));
        } else {
            MessageFacade.noRowsSelected();
        }
    }

    public void handleCopyPaste(FileFolderSelection selection) {
        FileFolder fl;
        Set<String> src = selection.getSelectedItems().keySet();
        String[] srcArr = src.toArray(new String[src.size()]);
        UserApi.copy(SecurityFacade.getLoggedInUser().getId(),
                SecurityFacade.getUserBeingViewed().getId(), srcArr, this.getCurrentFolder());
    }

    public void hardCopyFile(String remoteSourceFilePath,
                              String remoteDestinationFolder, String fileName) {
        try {
            File localSourceFile = new File(
                    JSFUtils.getLocalFilePath(remoteSourceFilePath));

            String remoteDestinationFilePath = (remoteDestinationFolder
                    .endsWith("/")) ? remoteDestinationFolder + fileName
                    : remoteDestinationFolder + "/" + fileName;

            File localDestinationFile = new File(
                    JSFUtils.getLocalFilePath(remoteDestinationFilePath));

            FileInputStream inputStream = new FileInputStream(localSourceFile);
            OutputStream out = new FileOutputStream(localDestinationFile);
            int read = 0;
            byte[] bytes = new byte[1024];

            while ((read = inputStream.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            inputStream.close();
            out.flush();
            out.close();

        } catch (Exception e) {
            MessageFacade.showException(e);
        }
    }

    public void hardCopyFolder(String remoteSourceFolderPath, String remoteDestinationFolder, String folderName) {
        File localSourceFolder = new File(JSFUtils.getLocalFilePath(remoteSourceFolderPath));
        String newRemoteDestinationFolder = (remoteDestinationFolder.endsWith("/"))
                ? remoteDestinationFolder + folderName : remoteDestinationFolder + "/" + folderName;
        for (File file : localSourceFolder.listFiles()) {
            String newRemoteSourceFolderPath = (remoteSourceFolderPath.endsWith("/"))
                    ? remoteSourceFolderPath + file.getName() : remoteSourceFolderPath + "/" + file.getName();
            if (file.isDirectory()) {
                hardCopyFolder(newRemoteSourceFolderPath, newRemoteDestinationFolder, file.getName());
            } else {
                hardCopyFile(newRemoteSourceFolderPath, newRemoteDestinationFolder, file.getName());
            }
        }

    }

    public boolean isItemCut(String path) {
        if (hasCopyPasteSelection()) {
            FileFolderSelection selection = (FileFolderSelection) SessionFacade
                    .getSessionObject(SessionItemKeys.FILE_FOLDER_SELECTION);
            if (selection.getPurpose().equals(SelectionPurpose.CUT)) {
                return selection.getSelectedItems().containsKey(path);
            }
        }
        return false;
    }

    public void cmdPaste_Click() {
        if (hasCopyPasteSelection()) {
            FileFolderSelection selection = (FileFolderSelection) SessionFacade
                    .getSessionObject(SessionItemKeys.FILE_FOLDER_SELECTION);
            if (selection.getPurpose() == SelectionPurpose.COPY) {
                handleCopyPaste(selection);
            } else if (selection.getPurpose() == SelectionPurpose.CUT) {
                handleCutPaste(selection);
            }
            this.clearCopyPasteFunction();
            this.loadFolderItems();
        }
    }

    public void jobCompletedAsyncRefresh() {
        String path = StringUtilities.DecryptSimple((String) this.getUrlParameter("path"), no.nels.portal.Config.getEncryptionSalt());
        logger.debug("path:" + path + ",currentFolder:" + getCurrentFolder());
        if (path.equals(getCurrentFolder())) {
            loadFolderItems();
        }
    }

    public void loadFolderItems() {
        try {

            List<FileFolder> items = UserApi.listItems(SecurityFacade
                    .getLoggedInUser().getId(), SecurityFacade
                    .getUserBeingViewed().getId(), getCurrentFolder());
            StringIndexedList flFldrs = new StringIndexedList();
            // populate the file folders
            for (FileFolder ff : items) {
                flFldrs.add(ff);
            }
            setFileFolders(new FileFolderGridModel(flFldrs));
        } catch (Exception ex) {
            MessageFacade.AddError("Error", ex.getMessage());
        }
    }

    public String getFileSizeForDisplay(long byteSize) {
        return FileUtils.getDisplayString(byteSize);
    }

    public void clearCopyPasteFunction() {
        if (SessionFacade.isSessionObjectSet(SessionItemKeys.FILE_FOLDER_SELECTION)) {
            SessionFacade
                    .removeSessionObject(SessionItemKeys.FILE_FOLDER_SELECTION);
        }
    }

    public boolean isProjectsList() {
        return this.getCurrentFolder().equals("Projects");
    }

    public String getPathNavigationString(String path) {
        return StringUtilities.EncryptSimple(path, no.nels.portal.Config.getEncryptionSalt());
    }
    public boolean isProjectTabActive() {
        return this.getCurrentFolder().startsWith("Projects");
    }
    public boolean isFileShuffleButtonsVisible() {

        // currently supported only for personal & project , not when viewing
        // another person's list
        return (isPersonal() || isProjectTabActive()) && !isProjectsList();
    }
}
