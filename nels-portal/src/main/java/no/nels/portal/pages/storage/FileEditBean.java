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
import org.primefaces.model.UploadedFile;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.util.ArrayList;

@ManagedBean(name = ManagedBeanNames.pages_storage_file_edit)
@ViewScoped
public class FileEditBean extends ASecureBean {

    private UploadedFile file;

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    private String path = "";
    private boolean isNew = false;
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
            path = StringUtilities.DecryptSimple(URLParametersFacade
                    .getMustUrLParameter("path"), no.nels.portal.Config.getEncryptionSalt());
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
        return "File Edit";
    }

    public void cmdCancel_Click() {
        NavigationFacade.closePopup();
    }

    public String getParentFolderUI() {
        return parentFolder.replace("/", " / ");
    }

    public void upload() {
        if (file != null) {

            try {
                UserApi.createFile(SecurityFacade.getLoggedInUser().getId(),
                        SecurityFacade.getUserBeingViewed().getId(),
                        parentFolder, file.getFileName(), file.getInputstream());
                MessageFacade.addInfo("Success",
                        "File uploaded successfully", true);
                NavigationFacade.closePopup();
            } catch (Exception e) {
                MessageFacade.showException(e);
            }

        } else {
            MessageFacade.AddError("File not provided");
        }
    }
}
