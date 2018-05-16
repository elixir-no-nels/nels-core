package no.nels.portal.pages;

import no.nels.commons.model.NelsUser;
import no.nels.commons.utilities.StringUtilities;
import no.nels.portal.Config;
import no.nels.portal.abstracts.ANelsBean;
import no.nels.portal.facades.MailFacade;
import no.nels.portal.facades.MessageFacade;
import no.nels.portal.facades.SecurityFacade;
import no.nels.portal.model.enumerations.ManagedBeanNames;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

@ManagedBean(name = ManagedBeanNames.pages_help_form)
@ViewScoped
public class HelpFormBean extends ANelsBean {

	private String name = "";
	private String email = "";
	private String subject = "";
	private String message = "";
	private String affiliation = "";

	private boolean isSent = false;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getPageTitle() {
		if (!isPostback()) {
			if (SecurityFacade.isUserLoggedIn()) {
				NelsUser usr = SecurityFacade.getLoggedInUser();
				this.name = usr.getIdpUser().getFullname();
				this.email = usr.getIdpUser().getEmail();
				this.affiliation = usr.getIdpUser().getAffiliation();
			}
		}
		return "NeLS help (support)";
	}

	public boolean validateInput() {
		boolean ret = true;
		if (name.equalsIgnoreCase("")) {
			MessageFacade.AddError("name not provided",
					"You have to submit your name");
			ret = false;
		}
		if (email.equalsIgnoreCase("")
				|| !StringUtilities.isValidEmailAddress(email)) {
			MessageFacade.AddError("invalid e-mail",
					"You have to provide a valid e-mail");
			ret = false;
		}
		if (subject.equalsIgnoreCase("")) {
			MessageFacade.AddError("subject not provided",
					"You have to provide a short subject");
			ret = false;
		}
		if (affiliation.equalsIgnoreCase("")) {
			MessageFacade.AddError("affiliation not provided",
					"You have to provide your institution name or affiliation");
			ret = false;
		}
		if (message.equalsIgnoreCase("")) {
			MessageFacade.AddError("message not provided",
					"You have to provide your message");
			ret = false;
		}
		return ret;
	}

	public void cmdSubmit_Click() {
		if (validateInput()) {
			try {
				String msg = "Request submitted through the NeLS portal\n\nName: ?name?\n\nAffiliation : ?affiliation?\n\nSubject : ?subject?\n\nMessage: \n\n?msg?\n\n\n+  +  +\nNeLS - Norwegian eInfrastructure for Life Sciences\nhttps://nels.bioinfo.no";
				msg = msg.replace("?name?", name).replace("?subject?", subject)
						.replace("?affiliation?", affiliation)
						.replace("?msg?", message);
				MailFacade.sendMail(email,
						new String[] { Config.getHelpdeskEmail() }, null, null,
						"NeLS request: " + subject, msg, false);
				isSent = true;
			} catch (Exception ex) {
				MessageFacade.AddError("mail sending failed", ex.getMessage());
			}
		}
	}

	public boolean getIsSent() {
		return isSent;
	}

	public void setIsSent(boolean isSent) {
		this.isSent = isSent;
	}

	public String getAffiliation() {
		return affiliation;
	}

	public void setAffiliation(String affiliation) {
		this.affiliation = affiliation;
	}

}
