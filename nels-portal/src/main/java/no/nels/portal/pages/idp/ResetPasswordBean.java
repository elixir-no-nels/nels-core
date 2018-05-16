package no.nels.portal.pages.idp;

import no.nels.client.Settings;
import no.nels.idp.core.facades.IdpFacade;
import no.nels.idp.core.model.db.NeLSIdpUser;
import no.nels.portal.abstracts.ASecureBean;
import no.nels.portal.facades.MessageFacade;
import no.nels.portal.facades.NavigationFacade;
import no.nels.portal.model.enumerations.IDPKeys;
import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.model.enumerations.URLParameterNames;
import no.nels.portal.utilities.JSFUtils;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

/**
 * Created by Kidane on 17.09.2015.
 */
@ManagedBean(name = ManagedBeanNames.pages_idp_nels_resetpassword)
@ViewScoped
public class ResetPasswordBean extends ASecureBean {


    private String newPassword;
    private String repeatPassword;
    private long userId;
    private String key;
    private boolean isReset;


    public String getPageTitle() {
        if (!isPostback()) {
            secure();
            this.registerRequestUrl();
            this.userId = Long.valueOf(this.getUrlParameter(URLParameterNames.ID).toString());
            this.key = this.getUrlParameter(URLParameterNames.KEY).toString();
            //we have to make sure the (id-key) pair matches exactly
            if (!Settings.isSettingMatch(IDPKeys.PASSWORD_RESET_KEY, userId, this.key)) {
                NavigationFacade.showInvalidOperation();
            }
        }
        return "Reset Password";
    }

    public void secure() {
        //user should not already be logged in
        /*
        if (SecurityFacade.isUserLoggedIn()) {
            NavigationFacade.showInvalidOperation();
        }
        */
    }

    public boolean isReset() {
        return isReset;
    }

    public void setReset(boolean isReset) {
        this.isReset = isReset;
    }

    public String getRepeatPassword() {
        return repeatPassword;
    }

    public void setRepeatPassword(String repeatPassword) {
        this.repeatPassword = repeatPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public void cmdSave_Click() {
        if (validateInput()) {
            if (null != IdpFacade.UpdatePassword(this.userId, newPassword)) {
                Settings.removeSetting(IDPKeys.PASSWORD_RESET_KEY, userId);
                isReset = true;
            } else {
                MessageFacade.addFatal("Error resetting user password");
            }


        }
    }

    private boolean validateInput() {
        boolean ret = true;

        //check newpwd and repeat pwd
        if (newPassword == null || newPassword.trim().equalsIgnoreCase("")) {
            MessageFacade.invalidInput("New Password not provided");
            ret = false;
        }

        if (repeatPassword == null || repeatPassword.trim().equalsIgnoreCase("")) {
            MessageFacade.invalidInput("Repeat Password not provided");
            ret = false;
        }
        if (!repeatPassword.equals(newPassword)) {
            MessageFacade.invalidInput("New Password does not match Repeat Password");
            ret = false;
        } else {
            //we have to check if the new password is same as the previous one
            NeLSIdpUser idpUser = IdpFacade.getById(this.userId);
            if (idpUser != null ) {
                if (Settings.getSetting(IDPKeys.PASSWORD_RESET_KEY, this.userId).split(",")[1].equals(IdpFacade.cipherPassword(newPassword))) {
                    MessageFacade.invalidInput("New Password must not be the same as previous password");
                    ret = false;
                }
            } else {
                MessageFacade.AddError("The user does not exist");
                ret = false;
            }
        }

        if (ret) {
            //validate pwd strength
            ret = JSFUtils.checkPasswordStrength(newPassword);

        }
        return ret;
    }

    public void cmdCancel_Click() {
        NavigationFacade.closePopup();
    }


}
