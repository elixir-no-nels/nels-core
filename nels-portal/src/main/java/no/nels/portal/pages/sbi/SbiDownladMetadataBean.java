package no.nels.portal.pages.sbi;

import no.nels.portal.Config;
import no.nels.portal.abstracts.ASecureBean;
import no.nels.portal.facades.NavigationFacade;
import no.nels.portal.facades.SecurityFacade;
import no.nels.portal.facades.URLParametersFacade;
import no.nels.portal.model.enumerations.ManagedBeanNames;

import javax.faces.bean.ManagedBean;

@ManagedBean(name = ManagedBeanNames.pages_sbi_download_metadata)
public class SbiDownladMetadataBean extends ASecureBean {
    @Override
    public void secure() {
        SecurityFacade.requireLogin();
    }

    @Override
    public String getPageTitle() {
        secure();
        String path = URLParametersFacade.getURLParameter("path");
        NavigationFacade.download(Config.getPublicApiUrl() + "/seek/sbi" + path + "/metadata");
        return "Download";
    }
}
