package no.nels.portal.pages.storage;

import no.nels.client.UserApi;
import no.nels.commons.abstracts.ASystemUser;
import no.nels.commons.model.systemusers.AdministratorUser;
import no.nels.commons.model.systemusers.HelpDeskUser;
import no.nels.commons.model.systemusers.NormalUser;
import no.nels.commons.utilities.StringUtilities;
import no.nels.portal.abstracts.ASecureBean;
import no.nels.portal.facades.MessageFacade;
import no.nels.portal.facades.NavigationFacade;
import no.nels.portal.facades.SecurityFacade;
import no.nels.portal.facades.URLParametersFacade;
import no.nels.portal.model.enumerations.ManagedBeanNames;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.util.ArrayList;

@ManagedBean(name = ManagedBeanNames.pages_storage_file_rename)
@ViewScoped
public class FileRenameBean extends ASecureBean {

	private String oldFilename;
	private String newFilename;
	private String parentFolder = "";

	public void secure() {
		ArrayList<ASystemUser> userTypes = new ArrayList<ASystemUser>() {
			{
				add(new AdministratorUser());
				add(new HelpDeskUser());
				add(new NormalUser());
			}
		};
		SecurityFacade.requireSystemUserType(userTypes);
	}

	public String getPageTitle() {
		if (!isPostback()) {
			secure();
			this.registerRequestUrl();
			parentFolder = StringUtilities.DecryptSimple(URLParametersFacade
                    .getMustUrLParameter("path"),no.nels.portal.Config.getEncryptionSalt());
			setOldFilename(StringUtilities.DecryptSimple(URLParametersFacade
                    .getMustUrLParameter("oldname"),no.nels.portal.Config.getEncryptionSalt()));
			//NavigationFacade.showInvalidOperation();
			
		}
		return "File/Folder Rename";
	}

	public void cmdCancel_Click() {
		NavigationFacade.closePopup();
	}
	
	public boolean validateInput() {
		if (newFilename == null || newFilename.trim().equalsIgnoreCase("")) {
			MessageFacade.invalidInput("New name not provided");
			return false;
		}
		return true;
	}

	public void cmdSave_Click() {
		if (validateInput()) {
			
			try {
				UserApi.renameFile(SecurityFacade.getLoggedInUser().getId(),SecurityFacade.getUserBeingViewed().getId(), parentFolder, oldFilename.trim(), newFilename.trim());
				MessageFacade.addInfo("Renamed successfully", "",true);
				NavigationFacade.closePopup();
			}
			catch (Exception ex) {
				if(ex.getMessage().toLowerCase().contains("FileAlreadyExistsException".toLowerCase())){
					MessageFacade.addFatal("Error", "Another file already exists at the same location");
				}
				else {
					MessageFacade.showException(ex);
				}
			}
		}
	}
	
	public void setOldFilename(String oldFilename) {
		this.oldFilename = oldFilename;
	}

	public String getOldFilename() {
		return oldFilename;
	}

	public void setNewFilename(String newFilename) {
		this.newFilename = newFilename;
	}

	public String getNewFilename() {
		return newFilename;
	}


}
