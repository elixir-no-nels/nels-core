package no.nels.portal.pages.idp;

import no.nels.commons.abstracts.ASystemUser;
import no.nels.commons.model.IDPUser;
import no.nels.commons.model.idps.NeLSIdp;
import no.nels.commons.model.systemusers.AdministratorUser;
import no.nels.commons.model.systemusers.HelpDeskUser;
import no.nels.commons.utilities.StringUtilities;
import no.nels.idp.core.facades.IdpFacade;
import no.nels.idp.core.model.db.NeLSIdpUser;
import no.nels.portal.Config;
import no.nels.portal.abstracts.ASecureBean;
import no.nels.portal.facades.*;
import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.model.enumerations.PageModes;
import no.nels.portal.model.enumerations.URLParameterNames;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.util.ArrayList;

@ManagedBean(name = ManagedBeanNames.pages_idp_nels_idpuser_edit)
@ViewScoped
public class NeLSIdpUserBean extends ASecureBean {

    /*Caution: The bean assumes e-mail is used as username*/

    private NeLSIdpUser idpUser;
    private String firstName, lastName, email, affiliation, initialPassword = StringUtilities.getRandomString(6);
    private boolean immediateNeLSProfileCreation = true;

    public boolean isImmediateNeLSProfileCreation() {
        return immediateNeLSProfileCreation;
    }

    public void setImmediateNeLSProfileCreation(boolean immediateNeLSProfileCreation) {
        this.immediateNeLSProfileCreation = immediateNeLSProfileCreation;
    }

    @Override
    public String getPageTitle() {
        if (!isPostback()) {
            secure();
            this.registerRequestUrl();
            if (this.getPageMode().equalsIgnoreCase(PageModes.Edit)) {
                this.idpUser = IdpFacade.getById(Long
                        .valueOf((String) this
                                .getUrlParameter(URLParameterNames.ID)));
                this.firstName = this.idpUser.getFirstName();
                this.lastName = this.idpUser.getLastName();
                this.email = this.idpUser.getEmail();
                this.affiliation = this.idpUser.getAffiliation();
            }
        }
        return "NeLS Idp User";
    }

    @Override
    public void secure() {
        ArrayList<ASystemUser> userTypes = new ArrayList<ASystemUser>() {
            {
                add(new AdministratorUser());
                add(new HelpDeskUser());
            }
        };
        SecurityFacade.requireSystemUserType(userTypes);
    }

    public boolean validateInput() {
        boolean ret = true;

        if (email.equalsIgnoreCase("") || !StringUtilities.isValidEmailAddress(email)) {
            MessageFacade.AddError("invalid e-mail", "You have to provide a valid e-mail");
            ret = false;
        } else {
            NeLSIdpUser usr = IdpFacade.getByEmail(email);
            if (usr != null) {
                if (this.getPageMode().equalsIgnoreCase(PageModes.New)) {
                    MessageFacade.AddError("already used e-mail",
                            "The email has already been used on another account.");
                    ret = false;
                } else if (this.getPageMode().equalsIgnoreCase(PageModes.Edit) && usr.getId() != getIdpUser().getId()) {
                    MessageFacade.AddError("already used e-mail",
                            "The updated email has already been used on another account.");
                    ret = false;
                }
            } else {
                usr = IdpFacade.getByUserName(email);
                if (usr != null) {
                    if (this.getPageMode().equalsIgnoreCase(PageModes.New)) {
                        MessageFacade.AddError("already used username",
                                "The username has already been used on another account.");
                        ret = false;
                    } else if (this.getPageMode().equalsIgnoreCase(PageModes.Edit) && usr.getId() != getIdpUser().getId()) {
                        MessageFacade.AddError("already used username",
                                "The updated username has already been used on another account.");
                        ret = false;
                    }
                }
            }
        }

        if (firstName.equalsIgnoreCase("")) {
            MessageFacade.AddError("First name not provided",
                    "You have to submit the First name");
            ret = false;
        }
        if (lastName.equalsIgnoreCase("")) {
            MessageFacade.AddError("Last name not provided",
                    "You have to submit the Last name");
            ret = false;
        }

        if (affiliation.equalsIgnoreCase("")) {
            MessageFacade.AddError("affiliation not provided",
                    "You have to provide the institution name or affiliation");
            ret = false;
        }
        if (this.getPageMode().equalsIgnoreCase(PageModes.New) && initialPassword.equalsIgnoreCase("")) {
            MessageFacade.AddError("blank password",
                    "You have to provide an initial password");
            ret = false;
        }

        return ret;
    }

    public void cmdCancel_Click() {
        NavigationFacade.closePopup();
    }

    public void cmdAdd_Click() {
        if (validateInput()) {
            NeLSIdpUser newIdpUser = IdpFacade.RegisterUser(email, initialPassword, firstName, lastName, email, affiliation);
            if (newIdpUser != null) {
                MessageFacade.addInfo("New User created ");

                if(isImmediateNeLSProfileCreation()) {
                    SecurityFacade.createNeLSProfile(new IDPUser(new NeLSIdp(), newIdpUser.getUsername(), newIdpUser.getFirstName() + " " + newIdpUser.getLastName(), newIdpUser.getEmail(), newIdpUser.getAffiliation()));
                }
                //send e-mail
                String newUserEmailMessage = Config.getMailNewNeLSIdpUser();
                newUserEmailMessage = newUserEmailMessage.replace("?fullName", firstName + " " + lastName).replace("?userName", email).replace("?tempPassword", initialPassword).replace("?webappUrl", Config.getApplicationRootURL());
                try {
                    MailFacade.sendMail(Config.getSenderEmail(), new String[]{email}, null, null, "Your NeLS Account Credentails", newUserEmailMessage, false);
                    LoggingFacade.logDebugInfo("new user credentials sent");
                } catch (Exception e) {
                    MessageFacade.addInfo("Sending e-mail", e.getMessage());
                    e.printStackTrace();
                }
                NavigationFacade.closePopup();
            } else {
                MessageFacade.addInfo("Adding user failed", "Something went wrong. Contact the system administrator");
            }
        }
    }

    public void cmdUpdate_Click() {
        if (validateInput()) {
            if(IdpFacade.UpdateUser(idpUser.getId(),email,firstName,lastName,email,affiliation)!= null) {
                MessageFacade.addInfo("Success ", "User details updated");
            }
            else
            {
             MessageFacade.addFatal("Error saving user details");
            }

        }
    }

    public NeLSIdpUser getIdpUser() {
        return idpUser;
    }

    public void setIdpUser(NeLSIdpUser idpUser) {
        this.idpUser = idpUser;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    public String getInitialPassword() {
        return initialPassword;
    }

    public void setInitialPassword(String initialPassword) {
        this.initialPassword = initialPassword;
    }
}
