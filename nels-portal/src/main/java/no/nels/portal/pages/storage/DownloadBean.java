package no.nels.portal.pages.storage;

import no.nels.client.APIProxy;
import no.nels.commons.utilities.StringUtilities;
import no.nels.portal.Config;
import no.nels.portal.abstracts.ASecureBean;
import no.nels.portal.facades.LoggingFacade;
import no.nels.portal.facades.NavigationFacade;
import no.nels.portal.facades.SecurityFacade;
import no.nels.portal.facades.URLParametersFacade;
import no.nels.portal.model.enumerations.ManagedBeanNames;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

@ManagedBean(name = ManagedBeanNames.pages_storage_file_download)
@ViewScoped
public class DownloadBean extends ASecureBean {

    @Override
    public void secure() {
        SecurityFacade.requireLogin();
    }

    @Override
    public String getPageTitle() {
        secure();
        try {
            String path = URLParametersFacade.isUrlParameterSet("path") ? StringUtilities.DecryptSimple(URLParametersFacade.getURLParameter("path"), no.nels.portal.Config.getEncryptionSalt()) : "Personal";

            String reference = APIProxy.getDownloadReference(SecurityFacade.getUserBeingViewed().getId(), path);

            NavigationFacade.download(Config.getDownloadUrl() + reference);
        } catch (Exception ex) {
            LoggingFacade.logDebugInfo(ex);
        }
        return "Download";
    }
}
