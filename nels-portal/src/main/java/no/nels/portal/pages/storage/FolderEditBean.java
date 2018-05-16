package no.nels.portal.pages.storage;

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
import no.nels.portal.model.enumerations.PageModes;
import no.nels.client.UserApi;
import org.apache.commons.lang.StringUtils;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.util.ArrayList;

@ManagedBean(name = ManagedBeanNames.pages_storage_folder_edit)
@ViewScoped
public class FolderEditBean extends ASecureBean {

	private String path = "";
	private boolean isNew = false;
	private String name;
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
			path = StringUtilities
					.DecryptSimple(URLParametersFacade
                            .getMustUrLParameter("path"),no.nels.portal.Config.getEncryptionSalt());
			if (this.getPageMode().equalsIgnoreCase(PageModes.New)) {
				isNew = true;
				parentFolder = path;
			} else if (this.getPageMode().equalsIgnoreCase(PageModes.New)) {
				isNew = false;
				parentFolder = StringUtils.substringBeforeLast(path, "/");
			} else {
				NavigationFacade.showInvalidOperation();
			}
		}
		return "Folder Edit";
	}

	public void cmdCancel_Click() {
		NavigationFacade.closePopup();
	}

	public void cmdSave_Click() {
		if (validateInput()) {
			try {
				UserApi.createFolder(SecurityFacade.getLoggedInUser().getId(),SecurityFacade.getUserBeingViewed().getId(),
						parentFolder, name.trim());
				MessageFacade.addInfo("Success","New folder created successfully",true);
				NavigationFacade.closePopup();
			}

			catch (Exception ex) {
				MessageFacade.showException(ex);
			}

		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean validateInput() {
		if (name == null || name.trim().equalsIgnoreCase("")) {
			MessageFacade.invalidInput("Folder name not provided");
			return false;
		}
		//unsupported characters
		if (!StringUtilities.isValidFileFolderName(name.trim())) {
			MessageFacade.invalidInput("Invalid character in folder name. Modify your input and try again");
			return false;
		}
		return true;
	}

	public String getParentFolderUI() {
		return parentFolder.replace("/", " / ");
	}
}
