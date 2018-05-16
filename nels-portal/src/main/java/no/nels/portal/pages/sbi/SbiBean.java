package no.nels.portal.pages.sbi;

import io.vertx.core.json.JsonObject;
import no.nels.client.APIProxy;
import no.nels.client.Settings;
import no.nels.client.UserApi;
import no.nels.client.model.SSHCredential;
import no.nels.client.sbi.SbiApiConsumer;
import no.nels.client.sbi.SbiException;
import no.nels.client.sbi.models.SbiData;
import no.nels.client.sbi.models.SbiDataSet;
import no.nels.client.sbi.models.SbiProject;
import no.nels.client.sbi.models.SbiSubtype;
import no.nels.commons.model.NumberIndexedList;
import no.nels.commons.model.ProjectUser;
import no.nels.commons.model.projectmemberships.NormalUserProjectMembership;
import no.nels.commons.utilities.StringUtilities;
import no.nels.portal.Config;
import no.nels.portal.facades.*;
import no.nels.portal.model.enumerations.*;
import no.nels.portal.model.sbi.SbiDataSetGridModel;
import no.nels.portal.model.sbi.SbiFileGridModel;
import no.nels.portal.model.sbi.SbiProjectGridModel;
import no.nels.portal.model.sbi.SbiSubTypeGridModel;
import no.nels.portal.navigation.StaticNavigationBean;
import no.nels.portal.pages.InteractionBean;
import no.nels.portal.session.SbiSessionBean;
import no.nels.portal.utilities.FileUtils;
import no.nels.portal.utilities.JSFUtils;
import no.nels.portal.utilities.SbiConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.ClientConfig;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.*;


/**
 * Created by Kidane on 06.01.2016.
 */
@ManagedBean(name = ManagedBeanNames.pages_sbi)
@ViewScoped
public class SbiBean extends InteractionBean implements Serializable {

    private static final Logger logger = LogManager.getLogger(SbiBean.class);
    private SbiProjectGridModel sbiProjectModel;
    private SbiDataSetGridModel sbiDataSetModel;
    private SbiSubTypeGridModel sbiSubTypeModel;
    private SbiFileGridModel sbiFileModel;
    private SbiData[] selectedSbiFiles;
    private String currentSbiPath;
    private int currentSbiPathLen;
    private String federatedId;
    private String dataSetId;
    private String name;


    private int size;


    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCurrentSbiPathLen(String currentSbiPath) {
        this.currentSbiPathLen = currentSbiPath.split("/").length;
    }

    public int getCurrentSbiPathLen() {
        return this.currentSbiPathLen;
    }


    public SbiSubTypeGridModel getSbiSubTypeModel() {
        return sbiSubTypeModel;
    }

    public void setSbiSubTypeModel(SbiSubTypeGridModel sbiSubTypeModel) {
        this.sbiSubTypeModel = sbiSubTypeModel;
    }

    public SbiDataSetGridModel getSbiDataSetModel() {
        return sbiDataSetModel;
    }

    public void setSbiDataSetModel(SbiDataSetGridModel sbiDataSetModel) {
        this.sbiDataSetModel = sbiDataSetModel;
    }


    public SbiProjectGridModel getSbiProjectModel() {
        return sbiProjectModel;
    }

    public void setSbiProjectModel(SbiProjectGridModel sbiProjectModel) {
        this.sbiProjectModel = sbiProjectModel;
    }

    public String getPathForSbiProject(String projectId) {
        return StringUtils.join(new String[]{SbiConstants.SBI, "/", projectId});
    }


    public void jobCompletedAsyncRefresh() {
        //reload nels-side
        loadNelsGridModel(this.getCurrentNelsPath());
        //reload sbi-side
        if (this.getCurrentSbiPathLen() > 3) {
            reloadSbiFileFolder();
        }
    }

    private void displayProjects() {
        logger.debug("loggedInUser:" + SecurityFacade.getLoggedInUser().getIdpUser().getIdpUsername());
        this.sbiProjectModel = new SbiProjectGridModel(SbiFacade.getSbiProjects(SecurityFacade.getLoggedInUser().getIdpUser().getIdpUsername()));
        logger.debug("projectModel:" + this.sbiProjectModel.getRowCount() + "," + SecurityFacade
                .getUserBeingViewed().getIdpUser().getIdpUsername());
    }

    private void displayDataSets() {
        String projectId = this.getLastPath(this.currentSbiPath);
        logger.debug("projectId:" + projectId);
        String projectName = SbiFacade.getSbiProject(Long.valueOf(projectId)).getName();
        this.setProjectNameInSbiSession(projectName);
        this.setProjectIdInSbiSession(projectId);
        this.sbiDataSetModel = new SbiDataSetGridModel(SbiFacade.getSbiDataSetListForProject(
                SecurityFacade.getUserBeingViewed().getIdpUser().getIdpUsername(), projectId));
    }

    private void displaySubtypes() {
        String dataSetId = this.getLastPath(this.currentSbiPath);
        this.setDataSetIdInSbiSession(dataSetId);
        String datasetName = SbiFacade.getSbiDataSet(SecurityFacade
                .getUserBeingViewed().getIdpUser().getIdpUsername(), Long.valueOf(this.getProjectIdFromSbiSession()), Long.valueOf(dataSetId)).getName();
        this.setDataSetNameInSbiSession(datasetName);
        NumberIndexedList dataSets = SbiFacade.getSbiDataSetListForProject(
                SecurityFacade.getUserBeingViewed().getIdpUser().getIdpUsername(), this.getProjectIdFromSbiSession());
        SbiDataSet dataSet = (SbiDataSet) dataSets.getById(Long.valueOf(dataSetId));
        String refDataSetId = dataSet.getDataSetId();
        this.setRefDataSetIdInSbiSession(refDataSetId);
        this.sbiSubTypeModel = new SbiSubTypeGridModel(SbiFacade.getSubTypeListForDataSet(SecurityFacade.getUserBeingViewed().getIdpUser().getIdpUsername(), this.getProjectIdFromSbiSession(), dataSetId));
    }

    private void displayFileFolders(int len) {
        this.federatedId = SecurityFacade.getUserBeingViewed().getIdpUser().getIdpUsername();
        this.dataSetId = this.getDataSetIdFromSbiSession();
        if (len == 4) { //it means the user clicked on the Subtype name
            String subtypeId = this.getLastPath(this.currentSbiPath);
            this.setSubtypeIdInSbiSession(subtypeId);
            List<SbiSubtype> subtypes = SbiFacade.getSbiSubtypes(this.federatedId, Long.valueOf(this.getProjectIdFromSbiSession()), Long.valueOf(this.getDataSetIdFromSbiSession()));
            String subtypeType = subtypes.stream().filter(s -> s.getId() == Long.valueOf(subtypeId)).findFirst().get().getType();
            this.setSubtypeTypeInSbiSession(subtypeType);
            loadSbiFileFolder(this.federatedId, this.dataSetId, this.getSubtypeIdFromSbiSession(), subtypeType);
        } else {
            loadSbiFileFolder(this.federatedId, this.dataSetId, this.getSubtypeIdFromSbiSession(), this.getSubtypeTypeFromSbiSession() + "/" + this.getFileParentPath());
        }
    }

    private void setProjectInfoInSbiSession(long projectId) {
        String projectName = SbiFacade.getSbiProject(projectId).getName();
        this.setProjectNameInSbiSession(projectName);
        this.setProjectIdInSbiSession(String.valueOf(projectId));
    }

    private void setDatasetInfoInSbiSession(long projectId, long datasetId) {
        SbiDataSet sbiDataSet = SbiFacade.getSbiDataSet(federatedId, projectId, datasetId);
        String datasetName = sbiDataSet.getName();
        String refDataSetId = sbiDataSet.getDataSetId();
        this.setRefDataSetIdInSbiSession(refDataSetId);
        this.setDataSetIdInSbiSession(String.valueOf(datasetId));
        this.setDataSetNameInSbiSession(datasetName);
    }

    private boolean hasUserAccessToProject(long projectId) {
        List<SbiProject> sbiProjects = new ArrayList<>();
        try {
            sbiProjects = SbiApiConsumer.getProjects(federatedId);
        } catch (ParseException | SbiException e) {
            logger.error(e.getLocalizedMessage());
        }
        return sbiProjects.stream().filter(p -> p.getId() == projectId).count() == 1;
    }

    private void initTwoPanel() {
        init("Projects", TabView.SBI);
        this.currentSbiPath = "StoreBioinfo";

        setCurrentSbiPathLen(this.currentSbiPath);
    }

    private void directNavigation() {
        logger.debug("directNavigation");

        try {
            SecurityFacade.setUserBeingViewed(SecurityFacade.getLoggedInUser().getId());
        } catch (Exception ex) {
            logger.warn(ex.getLocalizedMessage());
        }
        initTwoPanel();
        String ref = URLParametersFacade.getURLParameter(URLParameterNames.REF);
        String[] params = StringUtilities.base64Decode(ref).split(":");

        if (params.length != 3) {
            logger.warn("invalid params length. params:" + Arrays.toString(params));
            NavigationFacade.showInvalidOperation();
            return;
        }

        long projectId = Long.valueOf(params[0]);
        long datasetId = Long.valueOf(params[1]);
        String subtypeType = params[2];

        federatedId = SecurityFacade.getUserBeingViewed().getIdpUser().getIdpUsername();

        if (!hasUserAccessToProject(projectId)) {
            NavigationFacade.showAccessDenied();
        } else {
            setProjectInfoInSbiSession(projectId);
            setDatasetInfoInSbiSession(projectId, datasetId);
            List<SbiSubtype> sbiSubtypeList = SbiFacade.getSbiSubtypes(federatedId, projectId, datasetId);
            SbiSubtype sbiSubtype = sbiSubtypeList.stream().filter(s -> s.getType().equals(subtypeType)).findFirst().get();
            this.setSubtypeIdInSbiSession(String.valueOf(sbiSubtype.getId()));
            this.setSubtypeTypeInSbiSession(sbiSubtype.getType());
            this.currentSbiPath = "StoreBioinfo/" + projectId + "/" + datasetId + "/" + sbiSubtype.getId();
            logger.debug("projectId:" + projectId + ",datasetId:" + datasetId + "subtypeId:" + sbiSubtype.getId());
            setCurrentSbiPathLen(this.currentSbiPath);
            loadSbiFileFolder(SecurityFacade.getUserBeingViewed().getIdpUser().getIdpUsername(), this.getDataSetIdFromSbiSession(), this.getSubtypeIdFromSbiSession(), this.getSubtypeTypeFromSbiSession());
        }

    }

    @Override
    public String getPageTitle() {
        MessageFacade.handleDelayedMessages();

        SessionFacade.setSessionObject(SessionItemKeys.LAST_JOB_FETCH_TIME, 0L);

        if (!isPostback()) {
            secure();
            this.registerRequestUrl();
            int len;
            String currentNelsPath;
            if (URLParametersFacade.isUrlParameterSet(URLParameterNames.REF)) {
                directNavigation();
            } else {
                Optional<String> sbiPath = URLParametersFacade.getURLParameterValue(URLParameterNames.SBI);
                this.currentSbiPath = sbiPath.orElse("StoreBioinfo");
                setCurrentSbiPathLen(this.currentSbiPath);
                currentNelsPath = URLParametersFacade.getURLParameterValue(URLParameterNames.PATH).orElse("");
                len = getCurrentSbiPathLen();
                federatedId = SecurityFacade.getUserBeingViewed().getIdpUser().getIdpUsername();

                init(currentNelsPath, TabView.SBI);

                logger.debug("RECIEVED URL params:" + this.currentSbiPath + "----" + getCurrentNelsPath() + "---" + len);

                if (1 == len) {  //display projects
                    displayProjects();
                } else if (2 == len) { //display datasets on project view
                    displayDataSets();
                } else {
                    if (3 == len) { //display subtypes on dataset view
                        displaySubtypes();
                    } else if (len > 3) { //display folders and files on file view
                        displayFileFolders(len);
                    } else {
                        logger.error("Invalid len value:" + len);
                    }

                }
            }
            clearJobCheckTimeStamp();

        }
        return "SBI Import/Export";
    }

    public void showAddDataset(String closeJs) {

        String url = "/pages/sbi/sbi-adddataset.xhtml?projectId=" + StringUtilities.EncryptSimple(this.getProjectIdFromSbiSession(), no.nels.portal.Config.getEncryptionSalt());
        NavigationFacade.popPage(url, true, closeJs);
    }

    private void clearJobCheckTimeStamp() {
        //reload jobs for new-fetch
        SessionFacade.removeSessionObject(SessionItemKeys.LAST_JOB_FETCH_TIME);
    }

    private void gotoProjectView() {
        String projectId = this.currentSbiPath.split("/")[1];
        // TODO: this url needs to be encrypted.
        String url = "/pages/sbi/sbi.xhtml?path=" + getCurrentNelsPath() +
                "&sbi=" + "StoreBioinfo/" + projectId + "&name=" + this.getProjectNameFromSbiSession() + "&fromsbi=True";
        logger.debug("path:" + getCurrentNelsPath() + "projectView url:" + url);
        NavigationFacade.showPage(url, true);
    }

    private Boolean isDataSetLocked() {

        String dataSetId = this.getDataSetIdFromSbiSession();
        SbiDataSet dataSet = SbiFacade.getSbiDataSet(this.federatedId, Long.valueOf(this.getProjectIdFromSbiSession()), Long.valueOf(dataSetId));
        return dataSet.isLocked();
    }

    private void reloadSbiFileFolder() {
        String parentPath;
        if (getCurrentSbiPathLen() == 4) {
            parentPath = this.getSubtypeTypeFromSbiSession();
        } else {
            parentPath = this.getSubtypeTypeFromSbiSession() + "/" + this.getRelativePathUnderSubType();
        }
        logger.debug("federatedId: " + this.federatedId + ", datasetId:" + this.dataSetId + ", subType:" + this.getSubtypeIdFromSbiSession() + ",parentPath: " + parentPath);
        loadSbiFileFolder(this.federatedId, this.dataSetId, this.getSubtypeIdFromSbiSession(), parentPath);
    }

    private void loadSbiFileFolder(String federatedId, String dataSetId, String subType, String parentPath) {

        logger.debug("loadSbiFileFolder:" + federatedId + "," + dataSetId + "," + subType + "," + parentPath);

        this.sbiFileModel = new SbiFileGridModel(SbiFacade.fetchFiles(federatedId, this.getProjectIdFromSbiSession(), dataSetId, subType, parentPath));

        setSize(this.sbiFileModel.size());
    }

    private String getFileParentPath() {
        String[] fileStack = this.getFileStack();
        return fileStack[fileStack.length - 1];
    }


    public String[] getFileStack() {
        String[] pieces = this.getSplittedPath();
        if (pieces.length > 4) {
            String[] ret = new String[pieces.length - 4];
            ret[0] = pieces[4];
            for (int i = 1; i < ret.length; i++) {
                ret[i] = ret[i - 1] + "/" + pieces[i + 4];

            }
            return ret;
        } else {
            return new String[0];
        }
    }

    public String getPathForSbiDataSet(String dataSetName) {
        return this.currentSbiPath + "/" + dataSetName;
    }

    public String getPathForSbiSubType(String subTypeName) {
        return this.currentSbiPath + "/" + subTypeName;
    }

    public String getNewPathForFile(String fileName) {
        return this.currentSbiPath + "/" + fileName;
    }

    public String getPathNavigationString(String path) {
        return path;
    }

    @Override
    public void secure() {

    }

    public String getBlockHeader() {
        return SbiConstants.SBI;
    }


    private void setProjectNameInSbiSession(String projectName) {
        JSFUtils.getManagedBean(ManagedBeanNames.session_sbiBean, SbiSessionBean.class).setProjectName(projectName);
    }

    private String getProjectNameFromSbiSession() {
        return JSFUtils.getManagedBean(ManagedBeanNames.session_sbiBean, SbiSessionBean.class).getProjectName();
    }

    private void setProjectIdInSbiSession(String projectId) {
        JSFUtils.getManagedBean(ManagedBeanNames.session_sbiBean, SbiSessionBean.class).setProjectId(projectId);
    }

    private String getProjectIdFromSbiSession() {
        return JSFUtils.getManagedBean(ManagedBeanNames.session_sbiBean, SbiSessionBean.class).getProjectId();
    }

    private void setDataSetNameInSbiSession(String dataSetName) {
        JSFUtils.getManagedBean(ManagedBeanNames.session_sbiBean, SbiSessionBean.class).setDataSetName(dataSetName);
    }

    private String getDataSetNameFromSbiSession() {
        return JSFUtils.getManagedBean(ManagedBeanNames.session_sbiBean, SbiSessionBean.class).getDataSetName();
    }

    private void setDataSetIdInSbiSession(String dataSetId) {
        JSFUtils.getManagedBean(ManagedBeanNames.session_sbiBean, SbiSessionBean.class).setDataSetId(dataSetId);
    }

    private String getDataSetIdFromSbiSession() {
        return JSFUtils.getManagedBean(ManagedBeanNames.session_sbiBean, SbiSessionBean.class).getDataSetId();
    }

    private void setRefDataSetIdInSbiSession(String refDataSetId) {
        JSFUtils.getManagedBean(ManagedBeanNames.session_sbiBean, SbiSessionBean.class).setRefDataSetId(refDataSetId);
    }

    private String getRefDataSetIdFromSbiSession() {
        return JSFUtils.getManagedBean(ManagedBeanNames.session_sbiBean, SbiSessionBean.class).getRefDataSetId();
    }


    private void setSubtypeTypeInSbiSession(String subtypeType) {
        JSFUtils.getManagedBean(ManagedBeanNames.session_sbiBean, SbiSessionBean.class).setSubtypeType(subtypeType);
    }

    private String getSubtypeTypeFromSbiSession() {
        return JSFUtils.getManagedBean(ManagedBeanNames.session_sbiBean, SbiSessionBean.class).getSubtypeType();
    }

    private void setSubtypeIdInSbiSession(String subtypeId) {
        JSFUtils.getManagedBean(ManagedBeanNames.session_sbiBean, SbiSessionBean.class).setSubTypeId(subtypeId);
    }

    private String getSubtypeIdFromSbiSession() {
        return JSFUtils.getManagedBean(ManagedBeanNames.session_sbiBean, SbiSessionBean.class).getSubTypeId();
    }


    private String[] getSplittedPath() {

        return this.currentSbiPath.split("/");
    }

    public String getSubTypePath() {
        String[] paths = this.getSplittedPath();
        return paths[0] + "/" + paths[1] + "/" + paths[2] + "/" + paths[3];
    }

    public String[] getSbiNavStack() {

        String[] pieces = this.currentSbiPath.split("/");
        String[] ret = new String[pieces.length];
        ret[0] = pieces[0];
        for (int i = 1; i < pieces.length; i++) {
            ret[i] = ret[i - 1] + "/" + pieces[i];
        }
        return ret;
    }


    public String getIdentifier(String path) {
        int len = path.split("/").length;
        String identifier = "";
        if (1 == len) { // Display projects
            identifier = "StoreBioinfo";
        } else if (2 == len) {  //display datasets
            identifier = this.getProjectNameFromSbiSession();
        } else if (3 == len) { //display subtypes
            identifier = this.getDataSetNameFromSbiSession();
        } else if (4 <= len) { // display files
            identifier = this.getSubtypeTypeFromSbiSession();
            if (len > 4) {
                identifier = getLastPath(path);
            }
        }
        return identifier;

    }


    public String getFileSizeForDisplay(long byteSize) {
        return FileUtils.getDisplayString(byteSize);
    }

    private boolean isFileFolderExist(String name) {
        return false;
    }

    public boolean isValidOperation() {

//        for (SbiFile sbiFile : this.selectedSbiFiles) {
//            if (isItemExist(sbiFile.getName(), sbiFile.isFolder(), false)) {
//                return false;
//            }
//        }
        return true;
    }

    public boolean isNormalUserOperation() {

        boolean isNormalUser = false;
        long userId = SecurityFacade.getUserBeingViewed().getId();
        if (SessionFacade.isSessionObjectSet(SessionItemKeys.PROJECTS_OF_USER, userId) && getCurrentNelsPath().startsWith("Projects")) {
            HashMap<String, ProjectUser> projectsOfuser = (HashMap<String, ProjectUser>) SessionFacade.getSessionObject(SessionItemKeys.PROJECTS_OF_USER, userId);
            if (projectsOfuser.containsKey(getNavStack()[1])) {
                ProjectUser pu = projectsOfuser.get(getNavStack()[1]);
                isNormalUser = pu.getMembership().equals(new NormalUserProjectMembership()); //check if it's normal user
            }
        }
        if (isNormalUser) {
            for (SbiData sbiFile : this.selectedSbiFiles) {
                if (isItemExist(sbiFile.getName(), sbiFile.isFolder(), true)) {
                    return false;
                }
            }
            return true;
        } else {
            return true;
        }


    }

    public void transferToNels() {
        if (!isSourcePathValid(false)) {
            MessageFacade.AddError("Invalid transfer", "Data can only be transferred under subtype");
        } else if (!isDestPathValid(false)) {
            MessageFacade.AddError("Invalid transfer", "Data can only be transferred from Personal or a specific project");
        } else if (this.selectedSbiFiles == null || this.selectedSbiFiles.length == 0) {
            MessageFacade.noRowsSelected();
        } else if (!isValidOperation()) {
            MessageFacade.AddError("Object with the same name exists.");
        } else if (!isNormalUserOperation()) {
            MessageFacade.AddError("You don't have enough privilege to overwrite the object.");
        } else {
            //transfer data to NeLS
            String refDataSetId = getRefDataSetIdFromSbiSession();
            String dataSet = JSFUtils.getManagedBean(ManagedBeanNames.session_sbiBean, SbiSessionBean.class).getDataSetName();
            try {
                String nelsHost = APIProxy.getSshCredential(SecurityFacade
                        .getLoggedInUser().getId(), SecurityFacade.getUserBeingViewed().getId()).getHost();
                SSHCredential credential = UserApi.getSshCredential(SecurityFacade
                        .getLoggedInUser().getId(), SecurityFacade.getUserBeingViewed().getId());
                String scpUserName = credential.getUsername();
                String scpIdentityKey = credential.getSshKey().trim();
                String destinationFolder = getCurrentNelsPath();
                String parentPathOfSource = this.getRelativePathUnderSubType();
                long subtypeId = Long.valueOf(getSubtypeIdFromSbiSession());
                List<String> fileNames = new ArrayList<>();
                List<String> folderNames = new ArrayList<>();

                Arrays.stream(this.getSelectedSbiFiles()).forEach(fileFolder -> {
                    if (fileFolder.isFolder()) {
                        folderNames.add(fileFolder.getName());
                    } else {
                        fileNames.add(fileFolder.getName());
                    }
                });

                long nelsId = SecurityFacade.getLoggedInUser().getId();
                String subtypeType = this.getSubtypeTypeFromSbiSession();
                logger.debug("transferToNels parameters:" + nelsId + "," + nelsHost + "," + scpUserName + "," + refDataSetId + "," + subtypeType + "," + subtypeId + "," + destinationFolder + "," + parentPathOfSource);
                SbiFacade.fetchDataFromSbi(nelsId, nelsHost, scpUserName, scpIdentityKey, refDataSetId, dataSet, subtypeType, subtypeId, fileNames, folderNames, destinationFolder, parentPathOfSource);
            } catch (Exception e) {

                MessageFacade.AddError("sbi error." + e.getLocalizedMessage());
            }
        }
        clearJobCheckTimeStamp();
    }

    public boolean isSourcePathValid(boolean nelsToOther) {

        if (nelsToOther) { // nels to sbi
            return isNelsTransferPathValid();
        } else {
            return (this.currentSbiPathLen > 3);
        }
    }

    public boolean isDestPathValid(boolean nelsToOther) {
        if (nelsToOther) { // nels to sbi
            return (this.currentSbiPathLen > 3);

        } else {
            return isNelsTransferPathValid();
        }
    }

    @Override
    public void transferToOther() {

        if (!isSourcePathValid(true)) {
            MessageFacade.AddError("Invalid transfer", "Data can only be transferred from Personal or a specific project");
        } else if (!isDestPathValid(true)) {
            MessageFacade.AddError("Invalid transfer", "Data can only be transferred under subtype");
        } else if (super.getSelectedFileFolders() == null || super.getSelectedFileFolders().length == 0) {
            MessageFacade.noRowsSelected();
        } else if (isDataSetLocked()) {
            MessageFacade.AddError("", "The DataSet " + this.getDataSetNameFromSbiSession() + " is locked", true);
            gotoProjectView();
        } else {             //transfer data
            SbiSessionBean sbiSessionBean = JSFUtils.getManagedBean(ManagedBeanNames.session_sbiBean, SbiSessionBean.class);
            sbiSessionBean.setRelativePathUnderSubType(this.getRelativePathUnderSubType());
            String refDataSetId = sbiSessionBean.getRefDataSetId();
            String dataSet = sbiSessionBean.getDataSetName();
            String subtypeType = sbiSessionBean.getSubtypeType();
            String relativePath = sbiSessionBean.getRelativePathUnderSubType();

            String sourceFolder = getCurrentNelsPath();

            List<String> fileNames = new ArrayList<>();
            List<String> folderNames = new ArrayList<>();

            Arrays.stream(super.getSelectedFileFolders()).forEach(fileFolder -> {
                if (fileFolder.isFolder()) {
                    folderNames.add(fileFolder.getName());
                } else {
                    fileNames.add(fileFolder.getName());
                }
            });
            try {
                String nelsHost = APIProxy.getSshCredential(SecurityFacade
                        .getLoggedInUser().getId(), SecurityFacade.getUserBeingViewed().getId()).getHost();
                SSHCredential credential = UserApi.getSshCredential(SecurityFacade
                        .getLoggedInUser().getId(), SecurityFacade
                        .getLoggedInUser().getId());
                String scpUserName = credential.getUsername();
                String scpIdentityKey = credential.getSshKey().trim();

                long nelsId = SecurityFacade.getLoggedInUser().getId();
                long subtypeId = Long.valueOf(this.getSubtypeIdFromSbiSession());
                SbiFacade.transferDataToSbi(nelsId, nelsHost, scpUserName, scpIdentityKey, refDataSetId, dataSet, subtypeType, subtypeId, sourceFolder, relativePath, fileNames, folderNames);
                logger.debug(refDataSetId + "-" + subtypeType + "-" + subtypeId + "-" + sourceFolder + "-" + relativePath);

            } catch (Exception e) {
                MessageFacade.AddError("Some errors happened, please try later.");
            }
            clearJobCheckTimeStamp();
        }

    }

    @Override
    public boolean transferButtonVisible() {
        if (this.getCurrentSbiPathLen() >= 4 && !isHomeView() && !isProjectsHomeView()) {
            return true;
        } else {
            return false;
        }
    }

    public boolean transferToNelsButtonVisible() {
        if (!isProjectsHomeView() && !isHomeView() && this.getCurrentSbiPathLen() >= 4) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String getDestinationForView() {
        return "SBI";
    }


    private String getRelativePathUnderSubType() {
        String[] paths = this.getSplittedPath();
        if (paths.length == 4) {
            return "";
        } else {
            int len = this.getSubTypePath().length();
            logger.debug("subTypePath:" + this.getSubTypePath());
            return this.currentSbiPath.substring(len + 1);
        }
    }




    public SbiData[] getSelectedSbiFiles() {
        return selectedSbiFiles;
    }

    public void setSelectedSbiFiles(SbiData[] selectedSbiFiles) {
        this.selectedSbiFiles = selectedSbiFiles;
    }


    public SbiFileGridModel getSbiFileModel() {
        return sbiFileModel;
    }

    public String getCurrentSbiPath() {
        return currentSbiPath;
    }


    @Override
    public String getTargetPage() {
        return "sbi";
    }

    @Override
    public Map<String, String> getUrlParameters() {

        Map<String, String> map = new HashMap<>();
        String sbiPath = this.currentSbiPath;
        map.put("sbi", sbiPath);

        return map;

    }

    public boolean isSeekProject() {
        if (getProjectIdFromSbiSession() == null) {
            return false;
        } else if (Settings.isSettingFound(SettingKeys.SEEK_PROJECT, Long.valueOf(getProjectIdFromSbiSession())) && getCurrentSbiPathLen() > 3) {
            return true;
        } else {
            return false;
        }
    }

    public void uploadMetadataFile(String closeJs) {
        String projectId = getProjectIdFromSbiSession();
        String datasetId = getDataSetIdFromSbiSession();
        String subtype = getSubtypeTypeFromSbiSession();
        JSFUtils.getManagedBean(ManagedBeanNames.navigation_staticBean,
                StaticNavigationBean.class).uploadMetadataFile("/projects/" + projectId + "/datasets/" + datasetId + "/" + subtype,
                closeJs);
    }

    public String getDownloadMetadataPath() {
        String projectId = getProjectIdFromSbiSession();
        String datasetId = getDataSetIdFromSbiSession();
        String subtype = getSubtypeTypeFromSbiSession();
        return "/projects/" + projectId + "/datasets/" + datasetId + "/" + subtype;
    }

    public boolean showMetadataFile() {
        String path = getDownloadMetadataPath();

        JsonObject body = new JsonObject();
        body.put("method", "exist");
        Response checkResponse = ClientBuilder.newClient(new ClientConfig()).target(Config.getPublicApiUrl() + "/seek/sbi" + path + "/metadata/do").request().post(Entity.json(body.encode()));
        if (checkResponse.getStatus() == Response.Status.FOUND.getStatusCode()) {
            return true;
        } else {
            return false;
        }
    }

    public void removeMetadataFile() {
        String path = getDownloadMetadataPath();
        Response deleteResponse = ClientBuilder.newClient(new ClientConfig()).target(Config.getPublicApiUrl() + "/seek/sbi" + path + "/metadata").request().delete();
        if (deleteResponse.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
            try {
                MessageFacade.addInfo("Success", "The file is removed.");
                FacesContext.getCurrentInstance().getExternalContext().redirect(getRequestUrl());
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        } else {
            MessageFacade.addInfo("Can't delete file now.");
        }
    }

    public void showProjectMore(SbiProject project){
        JSFUtils.getManagedBean(ManagedBeanNames.navigation_staticBean,
                StaticNavigationBean.class).showSbiProjectInfo(project);
    }

}
