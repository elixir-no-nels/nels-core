package no.nels.portal.pages;

import no.nels.client.UserApi;
import no.nels.commons.abstracts.ASystemUser;
import no.nels.commons.model.NelsUser;
import no.nels.commons.model.systemusers.AdministratorUser;
import no.nels.commons.model.systemusers.HelpDeskUser;
import no.nels.portal.abstracts.ASecureBean;
import no.nels.portal.facades.NavigationFacade;
import no.nels.portal.facades.SecurityFacade;
import no.nels.portal.model.UserGridModel;
import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.utilities.UserTypeUtilities;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.util.ArrayList;

@ManagedBean(name = ManagedBeanNames.pages_helpdesk_home)
@ViewScoped
public final class HelpDeskHomeBean extends ASecureBean {
    private UserGridModel users = new UserGridModel(UserApi.getAllNelsUsers());
    private NelsUser selectedUser;

    public UserGridModel getUsers() {
        return users;
    }

    public NelsUser getSelectedUser() {
        return selectedUser;
    }

    public void setSelectedUser(NelsUser selectedUser) {
        this.selectedUser = selectedUser;
    }

    @Override
    public String getPageTitle() {
        if (!isPostback()) {
            this.secure();
            this.registerRequestUrl();
        }
        return "NeLS Help Desk Home";
    }

    @Override
    public void secure() {
        SecurityFacade.requireSystemUserType(new ArrayList<ASystemUser>(){
            {
                this.add(new HelpDeskUser());
                this.add(new AdministratorUser());
            }
        });
    }

    public void browseUser(long userId) {
        SecurityFacade.setUserBeingViewed(userId);
        NavigationFacade.goHome();
    }

    public String getSystemUserTypeName(long id) {
        return UserTypeUtilities.getUserTypeName(no.nels.commons.utilities.UserTypeUtilities.getUserType(id));
    }
}
