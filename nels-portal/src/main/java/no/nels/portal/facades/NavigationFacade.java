package no.nels.portal.facades;

import no.nels.commons.model.NelsUser;
import no.nels.commons.utilities.SecurityUtilities;
import no.nels.commons.utilities.StringUtilities;
import no.nels.portal.Config;
import no.nels.portal.model.Oauth2Config;
import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.model.enumerations.URLParameterNames;
import no.nels.portal.session.PopUpBean;
import no.nels.portal.session.RootRedirectBean;
import no.nels.portal.session.UserSessionBean;
import no.nels.portal.utilities.JSFUtils;
import no.nels.portal.utilities.UserTypeUtilities;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.Instant;

public class NavigationFacade {

    public static void showPage(String url, boolean isRelative) {
        showPage(url, isRelative, false);
    }

    public static void showPage(String url, boolean isRelative,
                                boolean mustOpenInTopWindow) {
        try {
            String absoluteUrl = isRelative ? JSFUtils.getAbsoluteUrl(url)
                    : url;
            if (mustOpenInTopWindow) {
                RootRedirectBean rootRedirect = JSFUtils.getManagedBean(
                        ManagedBeanNames.session_rootRedirectBean,
                        RootRedirectBean.class);
                rootRedirect.setUrl(absoluteUrl);
                rootRedirect.setHasRootRedirect(true);
            } else {
                FacesContext.getCurrentInstance().getExternalContext().redirect(absoluteUrl);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void download(String url) {
        showPage(url, false, false);
    }


    public static void popPage(String url, boolean isRelative, int width,
                               int height, String closeJs) {
        String absoluteUrl = isRelative ? JSFUtils.getAbsoluteUrl(url) : url;
        try {
            PopUpBean popUp = JSFUtils.getManagedBean(
                    ManagedBeanNames.session_popupBean, PopUpBean.class);
            popUp.setUrl(absoluteUrl);
            popUp.setHasPopup(true);
            popUp.setWidth(width);
            popUp.setHeight(height);
            popUp.setCloseJs(closeJs);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void popPage(String url, boolean isRelative) {
        // use default width and height = 1000x650
        popPage(url, isRelative, 1000, 650, "");
    }

    public static void popPage(String url, boolean isRelative, String closeJs) {
        popPage(url, isRelative, 1000, 650, closeJs);
    }

    public static void closePopup() {
        try {
            PopUpBean popUp = JSFUtils.getManagedBean(
                    ManagedBeanNames.session_popupBean, PopUpBean.class);
            popUp.setIsJustClosed(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showAccessDenied() {
        String relativeUrl = "/pages/common/access-denied.xhtml";
        showPage(relativeUrl, true, false);
    }

    public static String getInvalidOperationUrl() {
        return "/pages/common/invalid-operation.xhtml";
    }

    public static void showInvalidOperation() {
        showPage(getInvalidOperationUrl(), true, false);
    }

    public static void goHome() {
        UserSessionBean us = SecurityFacade.getUserSessionBean();
        //check for Oauth
        if (us.isOAuthFaciliator()) {
            Oauth2Config.Oauth2Client client = Config.getOauth2Config().getClient(us.getoAuthClientId());
            NelsUser user = SecurityFacade.getLoggedInUser();
            String dataJson = "{\"nels_id\":" + user.getId() + ", \"name\":\"" + user.getIdpUser().getFullname() + "\",\"user_type\":\"" + UserTypeUtilities.getUserTypeName(user.getSystemUserType()) + "\",\"timestamp\":" + Instant.now().toEpochMilli() + ",\"federated_id\":\"" + user.getIdpUser().getIdpUsername() + "\"}";
            String nels_token = SecurityUtilities.encrypt(Config.getOauth2Config().getEncryptionKey(), dataJson);
            String url = Config.getOauth2Config().getOauth2ServerUrl() + "?response_type=" + us.getoAuthResponseType() + "&client_id=" + client.getClientId() + "&redirect_uri=" + client.getRedirectUri() + "&scope=user&nels_token=" + nels_token;
            if (!us.getoAuthState().trim().equals("")) {
                url = StringUtilities.appendUrlParameter(url, URLParameterNames.STATE, us.getoAuthState());
            }
            //clear oauthmode
            us.setOAuthFaciliator(false);
            showPage(url, false);
        } else {
            String url = "/pages/file-browse.xhtml?path=" + StringUtilities.EncryptSimple("Personal", no.nels.portal.Config.getEncryptionSalt()) + "&isFolder=True";
            showPage(url, true, true);
        }
    }

}
