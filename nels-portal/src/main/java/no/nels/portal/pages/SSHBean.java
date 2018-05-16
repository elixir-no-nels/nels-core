package no.nels.portal.pages;

import no.nels.portal.*;
import no.nels.portal.abstracts.ASecureBean;
import no.nels.portal.facades.MessageFacade;
import no.nels.portal.facades.SecurityFacade;
import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.client.*;
import no.nels.client.model.*;
import no.nels.portal.utilities.JSFUtils;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

@ManagedBean(name = ManagedBeanNames.pages_ssh)
@ViewScoped
public class SSHBean extends ASecureBean {

	String userName = "";
	String sshKey = "";
	String host="";
	String fingerprint = "";

	public String getHost() {
		return host;
	}

	public String getUsername() {
		return userName;
	}

	public String getSshKey() {
		return sshKey;
	}

	public String getFingerprint() {
		return fingerprint;
	}


	public void secure() {
		SecurityFacade.requireLogin();
	}

	public void loadCredentials() {
		try {
			SSHCredential credentail = UserApi.getSshCredential(SecurityFacade
					.getLoggedInUser().getId(), SecurityFacade
					.getLoggedInUser().getId());
			host = credentail.getHost();
			userName = credentail.getUsername();
			sshKey = credentail.getSshKey();
			fingerprint = no.nels.portal.Config.getFingerprint();
		} catch (Exception ex) {
			MessageFacade.AddError("Error", ex.getMessage());
		}
	}

	public void cmdDownloadKey_Click(){
		try {
			JSFUtils.sendStringAsFile(getUsername() + "@" + getHost() + ".txt", getSshKey());
		}catch(Exception ex){
			MessageFacade.AddError("Error", ex.getMessage());
		}
	}

	public String getPageTitle() {
		if (!isPostback()) {
			secure();
			loadCredentials();
		}
		return "SSH Details";
	}

}
