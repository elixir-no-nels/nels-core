package no.nels.portal.pages.sbi;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import no.nels.client.sbi.models.SbiProject;
import no.nels.portal.abstracts.ASecureBean;
import no.nels.portal.facades.NavigationFacade;
import no.nels.portal.facades.SbiFacade;
import no.nels.portal.facades.SecurityFacade;
import no.nels.portal.facades.SessionFacade;
import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.model.enumerations.SessionItemKeys;
import no.nels.portal.utilities.FileUtils;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.util.ArrayList;
import java.util.Date;

@ManagedBean(name = ManagedBeanNames.pages_sbi_project_info)
@ViewScoped
public class SbiProjectInfo extends ASecureBean {

    SbiProject project = null;
    ArrayList members;

    public ArrayList getMembers() {
        return members;
    }

    public void secure() {
        SecurityFacade.requireLogin();
    }


    public String getPageTitle() {
        if (!isPostback()) {
            secure();
            showInfo();
        }
        return "Storebioinfo Project Details";
    }

    public void showInfo() {
        if (!SessionFacade.isSessionObjectSet(SessionItemKeys.SBI_PROJECT_DETAIL)) {
            NavigationFacade.showInvalidOperation();
        }
        this.project = SbiFacade.getSbiProject(((SbiProject) SessionFacade.getSessionObject(SessionItemKeys.SBI_PROJECT_DETAIL)).getId());

        JsonArray mms = SbiFacade.getSbiProjectMembers(this.project.getId());
        members = new ArrayList<String>();
        for (JsonElement member : mms) {
            members.add(member.getAsJsonObject().get("name").toString().replace("\"", "") + " (" + member.getAsJsonObject().get("role").toString().replace("\"", "") + ")");
        }

    }

    public String getQuotaName() {
        return this.project == null ? "" : this.project.getQuotaName();
    }

    public String getQuotaSize() {
        return this.project == null ? "" : FileUtils.getDisplayString(this.project.getQuotaSize());
    }

    public String getQuotaDiskUsage() {
        return this.project == null ? "" : FileUtils.getDisplayString(this.project.getDiskUsage());
    }

    public String getProjectName() {
        return this.project == null ? "" : this.project.getName();
    }

    public String getDescription() {
        return this.project == null ? "" : this.project.getDescription();
    }

    public String getUsedSize() {
        return this.project == null ? "" : FileUtils.getDisplayString(this.project.getUsedSize());
    }

    public String getContactPerson() {
        return this.project == null ? "" : this.project.getContactPerson();
    }

    public Date getCreationDate() {
        return this.project == null ? new Date() : this.project.getCreationDate();
    }

}
