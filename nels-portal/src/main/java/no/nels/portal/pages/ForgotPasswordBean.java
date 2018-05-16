package no.nels.portal.pages;


import no.nels.commons.utilities.StringUtilities;

import no.nels.idp.core.facades.IdpFacade;
import no.nels.idp.core.model.db.NeLSIdpUser;
import no.nels.portal.Config;
import no.nels.portal.abstracts.ANelsBean;


import no.nels.portal.facades.MessageFacade;
import no.nels.portal.facades.NavigationFacade;


import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.proxy.IDPProxy;


import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

/**
 * Created by Kidane on 17.09.2015.
 */
@ManagedBean(name = ManagedBeanNames.pages_idp_nels_forgotpassword)
@ViewScoped
public class ForgotPasswordBean extends ANelsBean {
    private NeLSIdpUser idpUser;
    private String email;
    private boolean isSubmitted;

    public String getPageTitle() {
        if (!isPostback()) {
        }
        return "Forgot Password";
    }

    public boolean isSubmitted() {
        return isSubmitted;
    }

    public void setSubmitted(boolean isSubmitted) {
        this.isSubmitted = isSubmitted;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void cmdSubmit_Click() {

        if (validateInput()) {
            this.idpUser = IdpFacade.getByEmail(email);
            if (null == this.idpUser) {
                MessageFacade.addFatal("This E-mail is not associated with any user");
                return;
            }
            if (IDPProxy.TriggerPasswordReset(this.idpUser,Config.getMailResetPassword())) {
                isSubmitted = true;
            } else {
                MessageFacade.AddError("Password reset failed. Contact system administrator");
            }

        }

    }

    private boolean validateInput() {
        boolean ret = true;
        if (email.equalsIgnoreCase("")
                || !StringUtilities.isValidEmailAddress(email)) {
            MessageFacade.AddError("invalid e-mail",
                    "You have to provide a valid e-mail");
            ret = false;
        }
        return ret;
    }

    public void cmdCancel_Click() {
        NavigationFacade.closePopup();
    }


}
