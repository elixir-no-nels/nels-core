package no.nels.portal.pages;

import no.nels.portal.Config;
import no.nels.portal.abstracts.ANelsBean;
import no.nels.portal.facades.NavigationFacade;
import no.nels.portal.facades.SecurityFacade;
import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.model.enumerations.Oauth2ResponseTypes;
import no.nels.portal.model.enumerations.URLParameterNames;
import no.nels.portal.session.UserSessionBean;
import no.nels.portal.session.UserSessionBean.GalaxyMode;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

@ManagedBean(name = ManagedBeanNames.pages_welcome)
@RequestScoped
public class WelcomeBean extends ANelsBean {

    public String getPageTitle() {
        if (!isPostback()) {
            registerRequestUrl();
            UserSessionBean us = SecurityFacade.getUserSessionBean();
            if (us != null) {
                handleGalaxyCallback(us);
                handleOauthFacilitator(us);
            }

        }
        return "Welcome to the NeLS portal!";
    }

    public void handleGalaxyCallback(UserSessionBean us) {
        String galaxyCallBackKey = "appCallbackUrl";
        if (this.hasUrlParameter(galaxyCallBackKey)) {
            us.setGalaxyCallBackUrl(this.getUrlParameter(galaxyCallBackKey)
                    .toString());
            GalaxyMode mode = this.getRequestUrl().toLowerCase()
                    .contains("browser") ? GalaxyMode.Get : GalaxyMode.Put;
            us.setGalaxyMode(mode);
            requireLogin();
        }
    }

    public void handleOauthFacilitator(UserSessionBean us) {
        if (this.hasUrlParameter(URLParameterNames.OAUTH_CLIENT)) {
            String oauthClientId = this.getUrlParameter(URLParameterNames.OAUTH_CLIENT).toString();

            if (Config.getOauth2Config().isClientIdValid(oauthClientId)) {
                us.setOAuthFaciliator(true);
                us.setoAuthClientId(oauthClientId);
                if (this.hasUrlParameter(URLParameterNames.STATE)) {
                    us.setoAuthState(this.getUrlParameter(URLParameterNames.STATE).toString());
                }
                if (this.hasUrlParameter(URLParameterNames.OAUTH_RESPONSE_TYPE)) {
                    String oauthClientResponseType = this.getUrlParameter(URLParameterNames.OAUTH_RESPONSE_TYPE).toString().toLowerCase();
                    if (Oauth2ResponseTypes.isValid(oauthClientResponseType)) {
                        us.setoAuthResponseType(oauthClientResponseType);
                    }
                }
                requireLogin();
            }
        }
    }

    public void requireLogin() {
        NavigationFacade.showPage("/pages/login.xhtml", true);
    }


    public String getGalaxyCallBackURL() {
        return SecurityFacade.getUserSessionBean().getGalaxyCallBackUrl();
    }
}
