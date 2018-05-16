package no.nels.portal.pages;

import no.nels.client.Settings;
import no.nels.commons.model.NelsUser;
import no.nels.portal.abstracts.ASecureBean;
import no.nels.portal.facades.NavigationFacade;
import no.nels.portal.facades.SecurityFacade;
import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.model.enumerations.SessionItemKeys;
import no.nels.portal.model.enumerations.SettingKeys;
import no.nels.portal.model.enumerations.TabView;
import no.nels.portal.session.ContentNavigatorBean;
import no.nels.portal.session.UserSessionBean;
import no.nels.portal.utilities.JSFUtils;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

@ManagedBean(name = ManagedBeanNames.pages_user_terms)
@ViewScoped
public class UserTermsBean extends ASecureBean {

    public void secure() {
    }


    public String getPageTitle() {
        if (!isPostback()) {
            secure();
        }
        return "Terms of usage";
    }

    public boolean isTermsOfUsageAccepted(){
        UserSessionBean userSessionBean = JSFUtils.getManagedBean(ManagedBeanNames.session_userSessionBean, UserSessionBean.class);
        return userSessionBean.isTermsOfUsageAccepted();
    }

    public void accept() {
        UserSessionBean userSessionBean = JSFUtils.getManagedBean(ManagedBeanNames.session_userSessionBean, UserSessionBean.class);
        Settings.setSetting(SettingKeys.TERMS_OF_USAGE_ACCEPTED_KEY, userSessionBean.getCurrentUser().getId(), Boolean.toString(true));
        userSessionBean.setSessionObject(SessionItemKeys.TERMS_OF_USAGE_ACCEPTED, true);
        NelsUser nelsUser = userSessionBean.getCurrentUser();
        SecurityFacade.setUserBeingViewed(nelsUser);
        JSFUtils.getManagedBean(ManagedBeanNames.session_contentNavigatorBean, ContentNavigatorBean.class).setCurrentFolder("Personal", TabView.NELS);
        NavigationFacade.goHome();
    }

    public void cancel() {

    }

}