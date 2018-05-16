package no.nels.portal.navigation;

import no.nels.portal.facades.NavigationFacade;
import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.model.enumerations.PageModes;
import no.nels.portal.model.enumerations.URLParameterNames;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

/**
 * Created by Kidane on 26.05.2015.
 */
@ManagedBean(name = ManagedBeanNames.navigation_idpBean)
@ViewScoped
public class IdpNavigationBean {

    public void viewIdpUsers(String closeJs){
        String url = "/pages/idp/idpusers.xhtml?" + URLParameterNames.Mode
                + "=" + PageModes.Process;
        NavigationFacade.popPage(url, true, closeJs);
    }

    public void viewIdpUserDetail(long idpUserId){
        String url ="/pages/idp/idpuserdetail.xhtml?id="+idpUserId;
        NavigationFacade.showPage(url,true,true);
    }

    public void addIdpUserDetail(String closeJs){
        String url = "/pages/idp/idpuser.xhtml?" + URLParameterNames.Mode
                + "=" + PageModes.New;
        NavigationFacade.popPage(url, true, closeJs);
    }

    public void editIdpUserDetail(long idpUserId,String closeJs){
        String url = "/pages/idp/idpuser.xhtml?" + URLParameterNames.Mode
                + "=" + PageModes.Edit +"&id=" + idpUserId;
        NavigationFacade.popPage(url, true, closeJs);
    }

    public void changePassword(String closeJs){
        String url = "/pages/idp/changepassword.xhtml";
        NavigationFacade.popPage(url, true, closeJs);
    }
}
