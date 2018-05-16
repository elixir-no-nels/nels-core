package no.nels.portal.pages.idp;

import no.nels.commons.abstracts.ASystemUser;
import no.nels.commons.model.systemusers.AdministratorUser;
import no.nels.commons.model.systemusers.HelpDeskUser;
import no.nels.commons.model.systemusers.NormalUser;
import no.nels.commons.utilities.StringUtilities;
import no.nels.idp.core.facades.IdpFacade;
import no.nels.idp.core.model.db.NeLSIdpUser;
import no.nels.portal.Config;
import no.nels.portal.abstracts.ASecureBean;
import no.nels.portal.facades.MessageFacade;
import no.nels.portal.facades.NavigationFacade;
import no.nels.portal.facades.SecurityFacade;
import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.navigation.IdpNavigationBean;
import no.nels.portal.proxy.IDPProxy;
import no.nels.portal.utilities.JSFUtils;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.util.ArrayList;

/**
 * Created by Kidane on 26.05.2015.
 */

@ManagedBean(name = ManagedBeanNames.pages_idp_nels_idpuser_view)
@ViewScoped
public class NeLSIdpUserViewBean extends ASecureBean {

    public String getPageTitle() {
        if (!isPostback()) {
            secure();
            this.registerRequestUrl();

            long idpId = Long.valueOf(this.getUrlParameter(no.nels.portal.model.enumerations.URLParameterNames.ID)
                    .toString());

            this.idpUser = IdpFacade.getById(idpId);
            if (this.idpUser == null) {
                NavigationFacade.showInvalidOperation();
            }
        }

        return "NeLS Idp User Details";
    }

    public void secure() {
        ArrayList<ASystemUser> userTypes = new ArrayList<ASystemUser>() {
            {
                add(new NormalUser());
                add(new AdministratorUser());
                add(new HelpDeskUser());
            }
        };
        SecurityFacade.requireSystemUserType(userTypes);
    }


    public NeLSIdpUser getIdpUser() {
        return idpUser;
    }

    public void setIdpUser(NeLSIdpUser idpUser) {
        this.idpUser = idpUser;
    }

    NeLSIdpUser idpUser;

    public void cmdEdit_Click() {
        JSFUtils.getManagedBean(ManagedBeanNames.navigation_idpBean,
                IdpNavigationBean.class).editIdpUserDetail(getIdpUser().getId(), "");
    }

    public void cmdChangePassword_Click() {

        JSFUtils.getManagedBean(ManagedBeanNames.navigation_idpBean,
                IdpNavigationBean.class).changePassword("");
    }

    public boolean isSelfView() {
        return getIdpUser().getUsername().equalsIgnoreCase(SecurityFacade.getLoggedInUser().getIdpUser().getIdpUsername());
    }

    public void cmdResetPassword_Click() {

        if (IDPProxy.TriggerPasswordReset(this.idpUser, Config.getMailAdminResetNeLSIdpUser())) {
            IdpFacade.UpdatePassword(this.idpUser.getId(), StringUtilities.getRandomString(6));
            MessageFacade.addInfo("Success", "The user's password has been reset successfully. An e-mail is sent to the user");
        } else {
            MessageFacade.addFatal("Failure", "An error occured while processing the password reset.");
        }

    }
}
