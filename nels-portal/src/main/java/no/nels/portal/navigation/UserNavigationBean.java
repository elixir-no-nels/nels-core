package no.nels.portal.navigation;

import no.nels.portal.abstracts.ASecureBean;
import no.nels.portal.facades.NavigationFacade;
import no.nels.portal.facades.PickerFacade;
import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.model.enumerations.PageModes;
import no.nels.portal.model.enumerations.URLParameterNames;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

@ManagedBean(name = ManagedBeanNames.navigation_userBean)
@ViewScoped
public class UserNavigationBean {

    public void viewUsers(String closeJs) {
        String url = "/pages/users/users.xhtml?" + URLParameterNames.Mode
                + "=" + PageModes.Process;
        NavigationFacade.popPage(url, true, closeJs);
    }

    public void editUsers(String closeJs) {
        String url = "/pages/users/users.xhtml?" + URLParameterNames.Mode + "=" + PageModes.Process;
        NavigationFacade.popPage(url, true, closeJs);
    }

    public void pickUsers(ASecureBean callerPageBean, String purposeIdentifier, String closeJs,
                          boolean onlyOne) {
        String pickerPageUrl = "/pages/users/users.xhtml";
        PickerFacade.launchPicker(pickerPageUrl, callerPageBean, onlyOne, purposeIdentifier, closeJs);
    }

    public void viewUserDetail(long userId) {
        String url = "/pages/users/userdetail-view.xhtml?id=" + userId;
        NavigationFacade.showPage(url, true, true);
    }

    public void viewUserTerms() {
        String url = "/pages/user-terms.xhtml";
        NavigationFacade.showPage(url, true, true);
    }
}
