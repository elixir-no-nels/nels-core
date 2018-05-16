package no.nels.portal.pages.idp;

import no.nels.idp.core.facades.IdpFacade;
import no.nels.idp.core.model.db.NeLSIdpUser;
import no.nels.portal.abstracts.ASecureBean;
import no.nels.portal.facades.MessageFacade;
import no.nels.portal.facades.NavigationFacade;
import no.nels.portal.facades.SecurityFacade;
import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.utilities.JSFUtils;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

/**
 * Created by Kidane on 17.09.2015.
 */
@ManagedBean(name = ManagedBeanNames.pages_idp_nels_changepassword)
@ViewScoped
public class ChangePassword extends ASecureBean {

    private NeLSIdpUser idpUser;
    private String currentPassword;
    private String originPassword;
    private String newPassword;
    private String repeatPassword;


    public String getPageTitle() {
        if (!isPostback()) {
            secure();
            this.idpUser = IdpFacade.getByUserName(SecurityFacade.getLoggedInUser().getIdpUser().getIdpUsername());
            this.originPassword = this.idpUser.getPassword();
        }
        return "Change Password";
    }

    public void secure() {

    }


    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getRepeatPassword() {
        return repeatPassword;
    }

    public void setRepeatPassword(String repeatPassword) {
        this.repeatPassword = repeatPassword;
    }

    public void cmdSave_Click() {

        if (validateInput()) {
            if(null != IdpFacade.UpdatePassword(idpUser.getId(), newPassword)) {
                MessageFacade.addInfo("Success ", "User password updated");
            }
            else {
                MessageFacade.addFatal("Error updating user password");
            }
        }
    }

    public void cmdCancel_Click() {
        NavigationFacade.closePopup();
    }


    private boolean validateInput() {
        boolean ret = true;
        //check current pwd
        if (currentPassword == null || currentPassword.trim().equalsIgnoreCase("")) {
            MessageFacade.invalidInput("Current Password not provided");
            ret = false;
        }
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
        }
        if (ret) {
            //validate pwd strength
            ret = JSFUtils.checkPasswordStrength(newPassword);
            if(ret){
                if(!IdpFacade.cipherPassword(currentPassword).equals(originPassword)){
                    MessageFacade.invalidInput("Original Password is not correct");
                    ret = false;
                }else {
                    ret = true;
                }
            }
        }
        return ret;
    }
}
