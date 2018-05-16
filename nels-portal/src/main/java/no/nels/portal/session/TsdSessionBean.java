package no.nels.portal.session;

import no.nels.portal.model.enumerations.ManagedBeanNames;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

@ManagedBean(name = ManagedBeanNames.session_tsdBean)
@SessionScoped
public final class TsdSessionBean {
    private String homeFolder;
    private String reference;
    private String userName;

    public String getHomeFolder() {
        return homeFolder;
    }

    public void setHomeFolder(String homeFolder) {
        this.homeFolder = homeFolder;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
