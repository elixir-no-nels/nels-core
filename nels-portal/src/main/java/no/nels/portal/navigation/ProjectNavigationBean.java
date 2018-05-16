package no.nels.portal.navigation;

import no.nels.commons.utilities.StringUtilities;
import no.nels.portal.Config;
import no.nels.portal.abstracts.ASecureBean;
import no.nels.portal.facades.NavigationFacade;
import no.nels.portal.facades.PickerFacade;
import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.model.enumerations.PageModes;
import no.nels.portal.model.enumerations.PickerType;
import no.nels.portal.model.enumerations.URLParameterNames;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

@ManagedBean(name = ManagedBeanNames.navigation_projectBean)
@ViewScoped
public class ProjectNavigationBean {
    public void createProject(String closeJs) {
        String url = "/pages/projects/project.xhtml?" + URLParameterNames.Mode + "=" + PageModes.New;
        NavigationFacade.popPage(url, true, closeJs);
    }

    public void editProject(String closeJs, String projectId) {
        String url = "/pages/projects/project.xhtml?" + URLParameterNames.Mode + "=" + PageModes.Edit + "&" + URLParameterNames.ID + "=" + projectId;
        NavigationFacade.popPage(url, true, closeJs);
    }

    public void editProjects(String closeJs) {
        String url = "/pages/projects/projects.xhtml?" + URLParameterNames.Mode + "=" + PageModes.Process;
        NavigationFacade.popPage(url, true, closeJs);
    }

    public void viewProjectDetail(long projectId) {
        String url = "/pages/projects/projectdetail-view.xhtml?id=" + projectId;
        NavigationFacade.showPage(url, true, true);
    }

    public void pickProjects(ASecureBean callerPageBean,String purposeIdentifier,  String closeJs, boolean onlyOne) {
    	String url = "/pages/projects/projects.xhtml";
    	PickerFacade.launchPicker(url, callerPageBean, onlyOne, purposeIdentifier, closeJs);
    }
}
