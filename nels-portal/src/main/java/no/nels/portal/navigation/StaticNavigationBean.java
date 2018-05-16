package no.nels.portal.navigation;

import no.nels.client.sbi.models.SbiProject;
import no.nels.commons.utilities.StringUtilities;
import no.nels.portal.facades.NavigationFacade;
import no.nels.portal.facades.SecurityFacade;
import no.nels.portal.facades.SessionFacade;
import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.model.enumerations.SessionItemKeys;
import no.nels.portal.model.enumerations.URLParameterNames;
import no.nels.portal.session.TsdSessionBean;
import no.nels.portal.session.UserSessionBean;
import no.nels.portal.utilities.GenericUtils;
import no.nels.portal.utilities.JSFUtils;
import no.nels.portal.utilities.SbiConstants;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import java.util.HashMap;
import java.util.Map;

@ManagedBean(name = ManagedBeanNames.navigation_staticBean)
@SessionScoped
public class StaticNavigationBean {

    public String getAbsoluteUrl(String relativeUrl) {
        return JSFUtils.getAbsoluteUrl(relativeUrl);
    }

    public void popUpRelative(String url) {
        NavigationFacade.popPage(url, true, "");
    }

    public void popUpAbsolute(String url) {
        NavigationFacade.popPage(url, false, "");
    }

    public void showTermsOfUse() {
        String url = "/static/terms.xhtml";
        NavigationFacade.popPage(url, true, "");
    }

    public void showRegistration() {
        String url = "/pages/registration.xhtml";
        NavigationFacade.popPage(url, true, "");
    }

    public void showTsdLogin() {
        TsdSessionBean tsdSessionBean = JSFUtils.getManagedBean(ManagedBeanNames.session_tsdBean, TsdSessionBean.class);
        if (tsdSessionBean.getReference() != null && !tsdSessionBean.getReference().isEmpty()) {
            showTsd(tsdSessionBean.getHomeFolder());
        } else {
            String url = "/pages/tsd/tsd-login.xhtml";
            NavigationFacade.popPage(url, true, "");
        }
    }

    public void showTsd(String homeFolder) {
        Map<String, String> parameterMap = new HashMap<>(2);
//        parameterMap.put(URLParameterNames.PATH, StringUtilities.EncryptSimple("",no.nels.portal.Config.getEncryptionSalt()));
//        parameterMap.put(URLParameterNames.TSD, StringUtilities.EncryptSimple(homeFolder,no.nels.portal.Config.getEncryptionSalt()));
        parameterMap.put(URLParameterNames.PATH, "");
        parameterMap.put(URLParameterNames.TSD, homeFolder);
        String url = GenericUtils.assembleRelativeURL("/pages/tsd/tsd-file.xhtml", parameterMap);
        NavigationFacade.showPage(url, true, true);
    }


    public void showLogin() {
        String url = "/pages/login.xhtml";
        NavigationFacade.showPage(url, true);
    }

    public void showForgotPassword() {

        String url = "/pages/forgotpassword.xhtml";
        NavigationFacade.popPage(url, true, "");
    }

    public void showSSHDetails() {
        String url = "/pages/ssh.xhtml";
        NavigationFacade.popPage(url, true, "");
    }

    public void showHelpForm() {
        String url = "/pages/help-form.xhtml";
        NavigationFacade.popPage(url, true, "");
    }

    public void showUserHome() {
        SecurityFacade.setUserBeingViewed(SecurityFacade.getLoggedInUser());
        String url = "/pages/file-browse.xhtml?path=" + StringUtilities.EncryptSimple("Personal", no.nels.portal.Config.getEncryptionSalt()) + "&isFolder=True";
        NavigationFacade.showPage(url, true);
    }

    public void showProjectsHome() {
        SecurityFacade.setUserBeingViewed(SecurityFacade.getLoggedInUser());
        String url = "/pages/projects-home.xhtml?path="
                + StringUtilities.EncryptSimple("Projects", no.nels.portal.Config.getEncryptionSalt()) + "&isFolder=False";
        NavigationFacade.showPage(url, true);
    }

    public void showSbiHome() {

        SecurityFacade.setUserBeingViewed(SecurityFacade.getLoggedInUser());
        Map<String, String> parameterMap = new HashMap<>(2);
        parameterMap.put(URLParameterNames.PATH, "");
        parameterMap.put(URLParameterNames.SBI, SbiConstants.SBI);
        String url = GenericUtils.assembleRelativeURL("/pages/sbi/sbi.xhtml", parameterMap);
        NavigationFacade.showPage(url, true);
    }

    public void uploadMetadataFile(String path, String closeJs) {
        String url = "/pages/sbi/metadata-upload.xhtml?path=" + path;
        NavigationFacade.popPage(url, true, closeJs);
    }

    public void showSbiProjectInfo(SbiProject project) {
        String url = "/pages/sbi/sbi-project-info.xhtml";
        SessionFacade.setSessionObject(SessionItemKeys.SBI_PROJECT_DETAIL,project);
        NavigationFacade.popPage(url, true);
    }


    public void editFolder(String path, boolean isNew, String closeJs) {
        String mode = (isNew) ? no.nels.portal.model.enumerations.PageModes.New
                : no.nels.portal.model.enumerations.PageModes.Edit;
        String url = "/pages/storage/folder-edit.xhtml?";
        url = StringUtilities.appendUrlParameter(url,
                no.nels.portal.model.enumerations.URLParameterNames.Mode, mode);
        url = StringUtilities.appendUrlParameter(url, "path",
                StringUtilities.EncryptSimple(path, no.nels.portal.Config.getEncryptionSalt()));
        NavigationFacade.popPage(url, true, closeJs);
    }

    public void editFile(String path, boolean isNew, String closeJs) {
        String mode = (isNew) ? no.nels.portal.model.enumerations.PageModes.New
                : no.nels.portal.model.enumerations.PageModes.Edit;
        String url = "/pages/storage/file-edit.xhtml?";
        url = StringUtilities.appendUrlParameter(url,
                no.nels.portal.model.enumerations.URLParameterNames.Mode, mode);
        url = StringUtilities.appendUrlParameter(url, "path",
                StringUtilities.EncryptSimple(path, no.nels.portal.Config.getEncryptionSalt()));
        NavigationFacade.popPage(url, true, closeJs);
    }

    public void renameFile(String path, String oldFilename, String closeJs) {
        String url = "/pages/storage/file-rename.xhtml?";
        url = StringUtilities.appendUrlParameter(url, "path",
                StringUtilities.EncryptSimple(path, no.nels.portal.Config.getEncryptionSalt()));
        url = StringUtilities.appendUrlParameter(url, "oldname",
                StringUtilities.EncryptSimple(oldFilename, no.nels.portal.Config.getEncryptionSalt()));
        NavigationFacade.popPage(url, true, closeJs);
    }

    public String getInvalidOperationUrl() {
        return NavigationFacade.getInvalidOperationUrl();
    }

    public void galaxyViewToNormal() {
        SecurityFacade.getUserSessionBean().setGalaxyMode(UserSessionBean.GalaxyMode.None);
        NavigationFacade.goHome();
    }

    public void showTestPage(String closeJs) {
        NavigationFacade.popPage("/pages/test.xhtml", true, closeJs);
    }
}
