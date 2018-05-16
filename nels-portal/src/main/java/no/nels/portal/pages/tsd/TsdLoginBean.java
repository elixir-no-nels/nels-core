package no.nels.portal.pages.tsd;

import no.nels.client.tsd.TsdApiConsumer;
import no.nels.client.tsd.TsdException;
import no.nels.portal.abstracts.ANelsBean;
import no.nels.portal.facades.MessageFacade;
import no.nels.portal.facades.NavigationFacade;
import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.navigation.StaticNavigationBean;
import no.nels.portal.session.TsdSessionBean;
import no.nels.portal.utilities.JSFUtils;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

@ManagedBean(name = ManagedBeanNames.pages_tsd_login)
@ViewScoped
public class TsdLoginBean extends ANelsBean{
    private String userName;
    private String password;
    private String otc;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getOtc() {
        return otc;
    }

    public void setOtc(String otc) {
        this.otc = otc;
    }

    @Override
    public String getPageTitle() {
        if (!isPostback()) {}
        return "TSD";
    }

    public void cmdConnect() {
        if (validateInput()) {
            try {
                String reference = TsdApiConsumer.connectTo(userName, password, otc);

                TsdSessionBean tsdSessionBean = JSFUtils.getManagedBean(ManagedBeanNames.session_tsdBean, TsdSessionBean.class);
                tsdSessionBean.setHomeFolder(this.userName.split("-")[0]);
                tsdSessionBean.setReference(reference);
                tsdSessionBean.setUserName(this.userName);

                StaticNavigationBean navigationBean = JSFUtils.getManagedBean(ManagedBeanNames.navigation_staticBean, StaticNavigationBean.class);
                navigationBean.showTsd(tsdSessionBean.getHomeFolder());
            } catch (TsdException e) {
                String message = e.getMessage();
                MessageFacade.AddError(message.contains(":") ? message.substring(message.indexOf(":") + 1) : "Internal error");
            }
        }
    }

    private boolean validateInput() {
        if (userName.isEmpty() || password.isEmpty() || otc.isEmpty()) {
            MessageFacade.AddError("You have to provide all fields");
            return false;
        }
        return true;
    }

    public void cmdCancel() {
        NavigationFacade.closePopup();
    }
}
