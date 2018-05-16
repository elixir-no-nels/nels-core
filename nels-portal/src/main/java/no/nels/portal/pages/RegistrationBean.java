package no.nels.portal.pages;

import no.nels.commons.utilities.StringUtilities;
import no.nels.idp.core.facades.IdpFacade;
import no.nels.portal.Config;
import no.nels.portal.abstracts.ANelsBean;
import no.nels.portal.facades.MailFacade;
import no.nels.portal.facades.MessageFacade;
import no.nels.portal.model.enumerations.ManagedBeanNames;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

@ManagedBean(name = ManagedBeanNames.pages_registration)
@ViewScoped
public class RegistrationBean extends ANelsBean {

	private String firstName = "";

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

	public String getUsageDescription() {
		return usageDescription;
	}

	public void setUsageDescription(String usageDescription) {
		this.usageDescription = usageDescription;
	}

	public boolean isSubmitted() {
		return isSubmitted;
	}

	public void setSubmitted(boolean isSubmitted) {
		this.isSubmitted = isSubmitted;
	}

	private String lastName = "";
	private String email = "";
	private String affiliation = "";
	private String usageDescription = "";

	private boolean isSubmitted = false;


	public String getPageTitle() {
		return "NeLS Identity Registration";
	}

	public boolean validateInput() {
		boolean ret = true;
		if (firstName.equalsIgnoreCase("")) {
			MessageFacade.AddError("First name not provided",
					"You have to submit your First name");
			ret = false;
		}
		if (lastName.equalsIgnoreCase("")) {
			MessageFacade.AddError("Last name not provided",
					"You have to submit your Last name");
			ret = false;
		}
		if (email.equalsIgnoreCase("")
				|| !StringUtilities.isValidEmailAddress(email)) {
			MessageFacade.AddError("invalid e-mail",
					"You have to provide a valid e-mail");
			ret = false;
		}
		else if(IdpFacade.getByEmail(email)!=null){
			MessageFacade.AddError("already registered e-mail",
					"You have previously registered.");
			ret = false;
		}
		if (affiliation.equalsIgnoreCase("")) {
			MessageFacade.AddError("affiliation not provided",
					"You have to provide your institution name or affiliation");
			ret = false;
		}
		if (usageDescription.equalsIgnoreCase("")) {
			MessageFacade.AddError("Usage description not provided",
					"You have to provide your Usage Description");
			ret = false;
		}
		return ret;
	}

	public void cmdSubmit_Click() {
		if (validateInput()) {
			try {
				String msg = Config.getMailRegistrationRequest();
				msg = msg.replace("?first_name?", firstName)
						.replace("?last_name?", lastName)
						.replace("?email?", email)
						.replace("?affiliation?", affiliation)
						.replace("?usage?", usageDescription);
				MailFacade.sendMail(email,
						Config.getRegistrationNotificationEmails(), null, null,
						"NeLS Identity Request" , msg, false);
				isSubmitted = true;
			} catch (Exception ex) {
				MessageFacade.AddError("mail sending failed", ex.getMessage());
			}
		}
	}
}
