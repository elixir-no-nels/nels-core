package no.nels.portal.session;

import no.nels.client.Settings;
import no.nels.client.UserApi;
import no.nels.commons.abstracts.AIDp;
import no.nels.commons.model.IDPUser;
import no.nels.commons.model.NelsProject;
import no.nels.commons.model.NelsUser;
import no.nels.commons.model.ProjectUser;
import no.nels.commons.model.idps.NeLSIdp;
import no.nels.commons.model.idps.NonNeLSIdp;
import no.nels.commons.model.projectmemberships.PIProjectMembership;
import no.nels.commons.model.systemusers.AdministratorUser;
import no.nels.commons.model.systemusers.HelpDeskUser;
import no.nels.commons.model.systemusers.NormalUser;
import no.nels.idp.core.facades.IdpFacade;
import no.nels.idp.core.model.db.NeLSIdpUser;
import no.nels.portal.Brokers.ProjectBroker;
import no.nels.portal.Config;
import no.nels.portal.facades.LoggingFacade;
import no.nels.portal.facades.SessionFacade;
import no.nels.portal.model.enumerations.*;
import no.nels.portal.utilities.JSFUtils;
import org.opensaml.saml2.core.Attribute;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml.SAMLCredential;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import java.util.HashMap;


@ManagedBean(name = ManagedBeanNames.session_userSessionBean)
@SessionScoped
public class UserSessionBean {

    public enum GalaxyMode {
        None, Put, Get
    }

    private HashMap<String, Object> sessionItems = new HashMap<String, Object>();

    private GalaxyMode galaxyMode = GalaxyMode.None;
    private NelsUser currentUser = null;
    private NelsUser userBeingViewed = null;
    private String galaxyCallBackUrl = "";

    public boolean isOAuthFaciliator() {
        return isOAuthFaciliator;
    }

    public void setOAuthFaciliator(boolean OAuthFaciliator) {
        isOAuthFaciliator = OAuthFaciliator;
    }

    private  boolean isOAuthFaciliator = false;

    public String getoAuthClientId() {
        return oAuthClientId;
    }

    public void setoAuthClientId(String oAuthClientId) {
        this.oAuthClientId = oAuthClientId;
    }

    private String oAuthClientId="";
    private String oAuthResponseType = Oauth2ResponseTypes.TOKEN;
    private String oAuthState="";

    public String getoAuthResponseType() {
        return oAuthResponseType;
    }

    public void setoAuthResponseType(String oAuthResponseType) {
        this.oAuthResponseType = oAuthResponseType;
    }

    public String getoAuthState() {
        return oAuthState;
    }

    public void setoAuthState(String oAuthState) {
        this.oAuthState = oAuthState;
    }

    private String getSAMLAttributeValue(SAMLCredential credential,
                                         String SAMLAttributeKey) {
        Attribute attr = credential.getAttributeByName(SAMLAttributeKey);
        String attrValue = attr.getAttributeValues().get(0).getDOM()
                .getTextContent();
        return attrValue;
    }

    public NelsUser getCurrentUser() {
        Authentication au = SecurityContextHolder.getContext()
                .getAuthentication();
        if (au != null && !(au instanceof AnonymousAuthenticationToken)
                && au.isAuthenticated()) {
            if (currentUser == null) {

                SAMLCredential credential = (SAMLCredential) SecurityContextHolder
                        .getContext().getAuthentication().getCredentials();
                // extract User details from SAML Token
                String email = getSAMLAttributeValue(credential,
                        FEIDEUserDetailKeys.Email);
                String primaryId = getSAMLAttributeValue(credential,
                        FEIDEUserDetailKeys.FeideId);
                String firstName = getSAMLAttributeValue(credential,
                        FEIDEUserDetailKeys.FirstName);
                String lastName = getSAMLAttributeValue(credential,
                        FEIDEUserDetailKeys.LastName);
                String affiliation = "Not Provided";
                try {
                    affiliation = getSAMLAttributeValue(credential,
                            FEIDEUserDetailKeys.OrgShortName);
                } catch (Exception ex) {
                }

                AIDp idp = new NonNeLSIdp();
                try {
                    if (getSAMLAttributeValue(credential, FEIDEUserDetailKeys.Issuer).equalsIgnoreCase("nels")) {
                        idp = new NeLSIdp();
                    }
                } catch (Exception ex) {
                }
                IDPUser idpUser = new IDPUser(idp, primaryId,
                        firstName + " " + lastName, email, affiliation);
                // get nels user details
                if (!UserApi.isUserRegistered(idpUser)) {
                    currentUser = UserApi.registerUser(idpUser);
                    if (currentUser != null) {
                        LoggingFacade
                                .logDebugInfo("new user registered to storage service. nelsid: "
                                        + Long.toString(currentUser.getId())
                                        + " name: "
                                        + currentUser.getIdpUser()
                                        .getFullname());
                    } else {
                        LoggingFacade
                                .logDebugInfo("new nels user registration failed on storage service.");
                        throw new NullPointerException("The current nels user should not be null");
                    }
                } else {
                    currentUser = UserApi.getNelsUser(idpUser);
                }
                userBeingViewed = currentUser;
                long nelsId = currentUser.getId();
                String fullName = currentUser.getIdpUser().getFullname();
                if (false == LoggingFacade.logUserLoginEvent(nelsId, fullName)) {
                    LoggingFacade.logDebugInfo("failed to add user login event. nelsId:" + nelsId + ",fullName:" + fullName);
                }
            }
        }
        return currentUser;
    }

    public String getApplicationRoot() {
        return JSFUtils.getApplicationRoot();
    }

    public GalaxyMode getGalaxyMode() {
        return this.galaxyMode;
    }

    public boolean isFromGalaxy() {
        return this.galaxyMode != GalaxyMode.None;
    }

    public boolean isGalaxyModePut() {
        return this.galaxyMode == GalaxyMode.Put;
    }

    public boolean isGalaxyModeGet() {
        return this.galaxyMode == GalaxyMode.Get;
    }

    public void setGalaxyMode(GalaxyMode galaxyMode) {
        this.galaxyMode = galaxyMode;
    }

    public String getGalaxyCallBackUrl() {
        return galaxyCallBackUrl;
    }

    public void setGalaxyCallBackUrl(String galaxyCallBackUrl) {
        this.galaxyCallBackUrl = galaxyCallBackUrl;
    }

    public boolean isPiOfProject(NelsProject project) {
        if (getCurrentUser() != null) {
            HashMap<String, ProjectUser> map = (HashMap<String, ProjectUser>) SessionFacade.getSessionObject(SessionItemKeys.PROJECTS_OF_USER, getCurrentUser().getId());
            ProjectUser projectUser = map.get(ProjectBroker.getStorageRoot(project));
            return projectUser.getMembership().equals(new PIProjectMembership());
        }
        return false;
    }

    public boolean isAdministrator() {
        if (getCurrentUser() != null) {
            return getCurrentUser().getSystemUserType().equals(
                    new AdministratorUser());
        }
        return false;
    }

    public boolean isHelpDesk() {
        if (getCurrentUser() != null) {
            return getCurrentUser().getSystemUserType().equals(
                    new HelpDeskUser());
        }
        return false;
    }

    public void setUserProjects(String userId, HashMap<String, ProjectUser> projects){
        setSessionObject(userId, projects);
    }

    public HashMap<String, ProjectUser> getUserProjects(String userId) {
        return (HashMap<String, ProjectUser>)getSessionObject(userId);
    }

    public boolean isNormalUser() {
        if (getCurrentUser() != null) {
            return getCurrentUser().getSystemUserType()
                    .equals(new NormalUser());
        }
        return false;
    }

    public boolean isNeLSIdpUser() {
        if (getCurrentUser() != null) {
            return getCurrentUser().getIdpUser().getIdp().getId() == new NeLSIdp().getId();
        }
        return false;
    }

    public NelsUser getUserBeingViewed() {
        return userBeingViewed;
    }

    public void setUserBeingViewed(NelsUser userBeingViewed) {
        this.userBeingViewed = userBeingViewed;
    }

	/*Session objects management */

    public boolean setSessionObject(String key, Object obj) {
        return this.sessionItems.put(key, obj) != null;
    }

    public Object getSessionObject(String key) {
        if (this.sessionItems.containsKey(key)) {
            return this.sessionItems.get(key);
        }
        return null;
    }

    public boolean removeSessionObject(String key) {
        return this.sessionItems.remove(key) != null;
    }

    public boolean isSessionObjectSet(String key) {
        return this.sessionItems.containsKey(key);
    }

    public String getNeLSIdpUserMyDetailLink() {
        return getApplicationRoot() + "/pages/idp/idpuserdetail.xhtml?id=" + getNeLSIdpUserId();
    }

    public String getNeLSIdpUserId() {
        NelsUser currentUser = getCurrentUser();
        if (currentUser != null) {
            NeLSIdpUser idpUser = IdpFacade.getByUserName(currentUser.getIdpUser().getIdpUsername());
            if (idpUser != null) {
                return String.valueOf(idpUser.getId());
            }
        }
        return "";
    }

    public boolean isTestMode() {
        return Config.getDeploymentMode() == DeploymentMode.TEST;
    }

    public boolean isProdMode() {
        return Config.getDeploymentMode() == DeploymentMode.PROD;
    }

    public boolean isProdPrallelMode() {
        return Config.getDeploymentMode() == DeploymentMode.PRODPARALLEL;
    }

    public boolean isTermsOfUsageAccepted() {
        if (isSessionObjectSet(SessionItemKeys.TERMS_OF_USAGE_ACCEPTED)) {
            return (Boolean) getSessionObject(SessionItemKeys.TERMS_OF_USAGE_ACCEPTED);
        }
        boolean isAccepted = Settings.isSettingFound(SettingKeys.TERMS_OF_USAGE_ACCEPTED_KEY, getCurrentUser().getId()) ?  Boolean.valueOf(Settings.getSetting(SettingKeys.TERMS_OF_USAGE_ACCEPTED_KEY, getCurrentUser().getId())) : false;
        setSessionObject(SessionItemKeys.TERMS_OF_USAGE_ACCEPTED, isAccepted);
        return isAccepted;
    }


}
