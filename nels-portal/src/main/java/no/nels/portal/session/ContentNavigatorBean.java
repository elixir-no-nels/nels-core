package no.nels.portal.session;

import no.nels.portal.facades.LoggingFacade;
import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.model.enumerations.TabView;
import no.nels.portal.utilities.JSFUtils;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

@ManagedBean(name = ManagedBeanNames.session_contentNavigatorBean)
@SessionScoped
public final class ContentNavigatorBean {
    private String currentFolder;
    private TabView view;

    public boolean isProjectTabActive() {
        return this.view == TabView.NELS && this.currentFolder.startsWith("Projects");
    }

    public boolean isPersonalTabActive() {
        return this.view == TabView.NELS && this.currentFolder.startsWith("Personal");
    }

    public boolean isSbiTabActive() {
        return this.view == TabView.SBI;
    }

    public boolean isTsdTabActive() {
        TsdSessionBean tsdSessionBean = JSFUtils.getManagedBean(ManagedBeanNames.session_tsdBean, TsdSessionBean.class);
        LoggingFacade.logDebugInfo("tsd homeFolder:" + tsdSessionBean.getHomeFolder() +", currentFolder:" + this.currentFolder);
        return tsdSessionBean.getHomeFolder() != null && !tsdSessionBean.getHomeFolder().isEmpty() && this.view == TabView.TSD ;
    }

    public void setCurrentFolder(String currentFolder, TabView view) {
        this.view = view;
        this.currentFolder = currentFolder;
    }


    public String getCurrentFolder() {
        return this.currentFolder;
    }

    public TabView getView() {
        return view;
    }
}
