package no.nels.portal.pages;

import no.nels.portal.abstracts.ASecureBean;
import no.nels.portal.facades.MessageFacade;
import no.nels.portal.facades.NavigationFacade;
import no.nels.portal.facades.SecurityFacade;
import no.nels.portal.model.enumerations.ManagedBeanNames;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

@ManagedBean(name = ManagedBeanNames.pages_test)
@ViewScoped
public class TestBean extends ASecureBean {

    public void secure() {
        SecurityFacade.requireLogin();
    }

    public String getPageTitle() {
        if (!isPostback()) {
            secure();
            this.registerRequestUrl();
        }
        return "Test page for . . . whatever";
    }

    public void cmdTest_Click() {
            MessageFacade.addInfo("hello", "some detail",true);
            NavigationFacade.closePopup();
    }
}
