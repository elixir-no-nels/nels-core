package no.nels.portal.pages.idp;

import no.nels.commons.abstracts.ASystemUser;
import no.nels.commons.model.systemusers.AdministratorUser;
import no.nels.commons.model.systemusers.HelpDeskUser;
import no.nels.idp.core.facades.IdpFacade;
import no.nels.idp.core.model.db.NeLSIdpUser;
import no.nels.portal.abstracts.ASecureBean;
import no.nels.portal.facades.SecurityFacade;
import no.nels.portal.model.IdpGridModel;
import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.navigation.IdpNavigationBean;
import no.nels.portal.utilities.JSFUtils;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.util.ArrayList;

/**
 * Created by Kidane on 26.05.2015.
 */
@ManagedBean(name= ManagedBeanNames.pages_idp_nels_idpusers)
@ViewScoped
public class NeLSIdpUsersBean extends ASecureBean {

    private IdpGridModel idpUsers = new IdpGridModel(IdpFacade.getAllIdpUsers());
    private NeLSIdpUser[] selectedIdpUsers;
    private NeLSIdpUser selectedIdpUser;

    public IdpGridModel getIdpUsers() {
        return idpUsers;
    }

    public void setIdpUsers(IdpGridModel idpUsers) {
        this.idpUsers = idpUsers;
    }

    public NeLSIdpUser[] getSelectedIdpUsers() {
        return selectedIdpUsers;
    }

    public void setSelectedIdpUsers(NeLSIdpUser[] selectedIdpUsers) {
        this.selectedIdpUsers = selectedIdpUsers;
    }

    public NeLSIdpUser getSelectedIdpUser() {
        return selectedIdpUser;
    }

    public void setSelectedIdpUser(NeLSIdpUser selectedIdpUser) {
        this.selectedIdpUser = selectedIdpUser;
    }


    public String getPageTitle() {
        if (!isPostback()) {
            secure();
            this.registerRequestUrl();
        }

        return "IDP Users";}

    public void secure() {
        ArrayList<ASystemUser> userTypes = new ArrayList<ASystemUser>() {
            {
                add(new AdministratorUser());
                add(new HelpDeskUser());
            }
        };
        SecurityFacade.requireSystemUserType(userTypes);
    }

    public void viewIdpUser(long idpUserId){
        JSFUtils.getManagedBean(ManagedBeanNames.navigation_idpBean, IdpNavigationBean.class).viewIdpUserDetail(idpUserId);
    }

}
