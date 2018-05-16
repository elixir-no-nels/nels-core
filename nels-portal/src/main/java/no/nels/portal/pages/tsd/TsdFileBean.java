package no.nels.portal.pages.tsd;

import no.nels.client.APIProxy;
import no.nels.client.UserApi;
import no.nels.client.model.FileFolder;
import no.nels.client.model.SSHCredential;
import no.nels.client.tsd.MasterApiConsumer;
import no.nels.client.tsd.TsdApiConsumer;
import no.nels.client.tsd.TsdException;
import no.nels.client.tsd.models.TsdFileFolder;
import no.nels.commons.model.ProjectUser;
import no.nels.commons.model.StringIndexedList;
import no.nels.commons.model.projectmemberships.NormalUserProjectMembership;
import no.nels.commons.utilities.StringUtilities;
import no.nels.portal.facades.MessageFacade;
import no.nels.portal.facades.SecurityFacade;
import no.nels.portal.facades.SessionFacade;
import no.nels.portal.facades.URLParametersFacade;
import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.model.enumerations.SessionItemKeys;
import no.nels.portal.model.enumerations.TabView;
import no.nels.portal.model.enumerations.URLParameterNames;
import no.nels.portal.model.tsd.TsdFileGridModel;
import no.nels.portal.navigation.StaticNavigationBean;
import no.nels.portal.pages.InteractionBean;
import no.nels.portal.session.TsdSessionBean;
import no.nels.portal.utilities.FileUtils;
import no.nels.portal.utilities.JSFUtils;
import no.nels.vertx.commons.constants.MqJobType;
import org.apache.commons.lang.StringUtils;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ManagedBean(name = ManagedBeanNames.pages_tsd_file)
@ViewScoped
public class TsdFileBean extends InteractionBean {

    private TsdFileGridModel tsdFileModel;
    private TsdFileFolder[] selectedTsdFileFolders;
    private String currentTsdPath;


    @Override
    public String getPageTitle() {

        SessionFacade.setSessionObject(SessionItemKeys.LAST_JOB_FETCH_TIME, 0L);
        if (!isPostback()) {
            secure();


            String tsdPath = URLParametersFacade.getMustUrLParameter(URLParameterNames.TSD);
            //this.currentTsdPath = StringUtilities.DecryptSimple(tsdPath,no.nels.portal.Config.getEncryptionSalt());
            this.currentTsdPath = tsdPath;
            this.loadTsdGridModel(this.currentTsdPath);

            String nelsPath = URLParametersFacade.getMustUrLParameter(URLParameterNames.PATH);
            //String currentNelsPath = StringUtilities.DecryptSimple(nelsPath, no.nels.portal.Config.getEncryptionSalt());
            String currentNelsPath = nelsPath;

            init(currentNelsPath, TabView.TSD);

            this.registerRequestUrl();

            clearJobCheckTimeStamp();

        }
        return "TSD Import/Export";
    }

    @Override
    public void secure() {

    }

    private void clearJobCheckTimeStamp(){
        //reload jobs for new-fetch
        SessionFacade.removeSessionObject(SessionItemKeys.LAST_JOB_FETCH_TIME);
    }



    public void jobCompletedAsyncRefresh() {

        //reload nels-side
        loadNelsGridModel(this.getCurrentNelsPath());

        //reload tsd-side
        loadTsdGridModel(this.currentTsdPath);

    }

    private String getRealCurrentTsdPath() {
        TsdSessionBean tsdSessionBean = JSFUtils.getManagedBean(ManagedBeanNames.session_tsdBean, TsdSessionBean.class);
        if (this.currentTsdPath.equalsIgnoreCase(tsdSessionBean.getHomeFolder())) {
            return FileSystems.getDefault().getSeparator() + tsdSessionBean.getHomeFolder();
        } else {
            return FileSystems.getDefault().getSeparator() + this.currentTsdPath;
        }
    }

    private void loadTsdGridModel(String path) {
        TsdSessionBean tsdSessionBean = JSFUtils.getManagedBean(ManagedBeanNames.session_tsdBean, TsdSessionBean.class);
        String reference = tsdSessionBean.getReference();
        String userName = tsdSessionBean.getUserName();

        String homeFolder = tsdSessionBean.getHomeFolder();
        try {
            List<TsdFileFolder> list;
            if (path.equalsIgnoreCase(homeFolder)) {
                list = TsdApiConsumer.navigateToHome(reference, userName);
            } else {
                list = TsdApiConsumer.navigateTo(reference, userName, path.substring(path.indexOf(FileSystems.getDefault().getSeparator()) + 1));
            }

            StringIndexedList fileFolders = new StringIndexedList();
            list.stream().forEach(fileFolders::add);
            this.tsdFileModel = new TsdFileGridModel(fileFolders);
        } catch (TsdException e) {
            String message = e.getMessage();
            if (StringUtils.isEmpty(message)) {
                message = "Internal error";
            } else {
                if (message.contains(":")) {
                    message = message.substring(message.indexOf(":") + 1);
                }
            }

            MessageFacade.AddError(message);
        }
    }

    public String[] getTsdNavStack() {

        String[] pieces = this.currentTsdPath.split("/");
        String[] ret = new String[pieces.length];
        ret[0] = pieces[0];
        for (int i = 1; i < pieces.length; i++) {
            ret[i] = ret[i - 1] + "/" + pieces[i];
        }
        return ret;
    }


    public String encryptPath(String path) {
        return StringUtilities.EncryptSimple(path, no.nels.portal.Config.getEncryptionSalt());
    }

    public String getFileSizeForDisplay(long byteSize) {
        return FileUtils.getDisplayString(byteSize);
    }

    public void transferToNels() {
        clearJobCheckTimeStamp();
        if (!isSourcePathValid(false)) {
            MessageFacade.AddError("Invalid transfer", "Data can only be transferred from export");
        } else if (!isDestPathValid(false)) {
            MessageFacade.AddError("Invalid transfer", "Data can only be transferred under Personal or a specific project");
        } else if (this.selectedTsdFileFolders == null || this.selectedTsdFileFolders.length == 0) {
            MessageFacade.noRowsSelected();
        } else if (!isValidOperation()) {
            MessageFacade.AddError("Object with the same name exists.");
        } else if (!isNormalUserOperation()) {
            MessageFacade.AddError("You don't have enough privilege to overwrite the item.");
        } else {
            try {
                List<String> filesList = new ArrayList<>();
                List<String> foldersList = new ArrayList<>();
                for (TsdFileFolder item : this.selectedTsdFileFolders) {
                    if (item.isFolder()) {
                        foldersList.add(item.getName());
                    } else {
                        filesList.add(item.getName());
                    }
                }

                final TsdSessionBean tsdSessionBean = JSFUtils.getManagedBean(ManagedBeanNames.session_tsdBean, TsdSessionBean.class);
                String tsdUserName = tsdSessionBean.getUserName();
                String reference = tsdSessionBean.getReference();

                String nelsHost = APIProxy.getSshCredential(SecurityFacade
                        .getLoggedInUser().getId(), SecurityFacade.getUserBeingViewed().getId()).getHost();
                SSHCredential credential = UserApi.getSshCredential(SecurityFacade
                        .getLoggedInUser().getId(), SecurityFacade.getUserBeingViewed().getId());
                String sshUserName = credential.getUsername();
                String sshIdentityKey = credential.getSshKey().trim();

                MasterApiConsumer.addPushDataToNelsJob(
                        SecurityFacade.getLoggedInUser().getId(), MqJobType.TSD_PUSH.getValue(),
                        sshUserName, nelsHost, sshIdentityKey, getCurrentNelsPath(),
                        tsdUserName, reference, this.getRealCurrentTsdPath(), filesList, foldersList);
            } catch (Exception ex) {
                MessageFacade.addInfo("Failed to transfer.");
            }
        }
    }

    public void disconnectTsd() {
        TsdSessionBean tsdSessionBean = JSFUtils.getManagedBean(ManagedBeanNames.session_tsdBean, TsdSessionBean.class);
        String reference = tsdSessionBean.getReference();
        String userName = tsdSessionBean.getUserName();
        try {
            TsdApiConsumer.disconnectTsdSession(reference, userName);
        } catch (TsdException e) {}

        tsdSessionBean.setHomeFolder("");
        tsdSessionBean.setReference("");
        tsdSessionBean.setUserName("");
        StaticNavigationBean navigationBean = JSFUtils.getManagedBean(ManagedBeanNames.navigation_staticBean, StaticNavigationBean.class);
        navigationBean.showUserHome();
    }


    public TsdFileFolder[] getSelectedTsdFileFolders() {
        return selectedTsdFileFolders;
    }

    public void setSelectedTsdFileFolders(TsdFileFolder[] selectedTsdFileFolders) {
        this.selectedTsdFileFolders = selectedTsdFileFolders;
    }


    public TsdFileGridModel getTsdFileModel() {
        return tsdFileModel;
    }

    public String getCurrentTsdPath() {
        return currentTsdPath;
    }


    @Override
    public String getTargetPage() {
        return "tsd-file";
    }

    @Override
    public Map<String, String> getUrlParameters() {
        Map<String, String> map = new HashMap<>();
        String tsdPath = getCurrentTsdPath();
        map.put("tsd", tsdPath);
        return map;
    }

    public boolean isSourcePathValid(boolean nelsToOther) {

        if (nelsToOther) { // nels to tsd
            return isNelsTransferPathValid();
        } else {
            return getTsdNavStack().length >= 2 && getTsdNavStack()[1].endsWith("export");

        }
    }

    public boolean isDestPathValid(boolean nelsToOther) {
        if (nelsToOther) { // nels to tsd
            return getTsdNavStack().length >= 2 && getTsdNavStack()[1].endsWith("import");

        } else {
            return isNelsTransferPathValid();
        }
    }

    @Override
    public void transferToOther() {
        clearJobCheckTimeStamp();
        if (!isSourcePathValid(true)) {
            MessageFacade.AddError("Invalid transfer", "Data can only be transferred from Personal or a specific project");
        } else if (!isDestPathValid(true)) {
            MessageFacade.AddError("Invalid transfer", "Data can only be transferred under import");
        } else if (super.getSelectedFileFolders() == null || super.getSelectedFileFolders().length == 0) {
            MessageFacade.noRowsSelected();
        } else {
            try {
                List<String> filesList = new ArrayList<>();
                List<String> foldersList = new ArrayList<>();
                for (FileFolder item : super.getSelectedFileFolders()) {
                    if (item.isFolder()) {
                        foldersList.add(item.getName());
                    } else {
                        filesList.add(item.getName());
                    }
                }

                final TsdSessionBean tsdSessionBean = JSFUtils.getManagedBean(ManagedBeanNames.session_tsdBean, TsdSessionBean.class);
                String tsdUserName = tsdSessionBean.getUserName();
                String reference = tsdSessionBean.getReference();

                String nelsHost = APIProxy.getSshCredential(SecurityFacade
                        .getLoggedInUser().getId(), SecurityFacade.getUserBeingViewed().getId()).getHost();
                SSHCredential credential = UserApi.getSshCredential(SecurityFacade
                        .getLoggedInUser().getId(), SecurityFacade.getUserBeingViewed().getId());
                String sshUserName = credential.getUsername();
                String sshIdentityKey = credential.getSshKey().trim();

                MasterApiConsumer.addPullDataFromNelsJob(
                        SecurityFacade.getLoggedInUser().getId(), MqJobType.TSD_PULL.getValue(),
                        sshUserName, nelsHost, sshIdentityKey, getCurrentNelsPath(), filesList, foldersList,
                        tsdUserName, reference, this.getRealCurrentTsdPath());
            } catch (Exception ex) {
                MessageFacade.AddError("Transfer is failed");
            }


        }
    }

    @Override
    public boolean transferButtonVisible() {
        return true;
    }

    public boolean isValidOperation() {
        for (TsdFileFolder tsdFileFolder : this.selectedTsdFileFolders) {
            if (isItemExist(tsdFileFolder.getName(), tsdFileFolder.isFolder(), false)) {
                return false;
            }
        }
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
            for (TsdFileFolder tsdFileFolder : this.selectedTsdFileFolders) {
                if (isItemExist(tsdFileFolder.getName(), tsdFileFolder.isFolder(), true)) {
                    return false;
                }
            }
            return true;
        } else {
            return true;
        }


    }

    @Override
    public String getDestinationForView() {
        return "Tsd";
    }
}
