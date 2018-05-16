package no.nels.portal.pages.sbi;

import no.nels.client.sbi.SbiApiConsumer;
import no.nels.client.sbi.SbiException;
import no.nels.client.sbi.models.SbiDataSet;
import no.nels.client.sbi.models.SbiDataSetType;
import no.nels.commons.model.NelsUser;
import no.nels.commons.utilities.StringUtilities;
import no.nels.portal.abstracts.ASecureBean;
import no.nels.portal.facades.MessageFacade;
import no.nels.portal.facades.NavigationFacade;
import no.nels.portal.facades.URLParametersFacade;
import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.session.UserSessionBean;
import no.nels.portal.utilities.JSFUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.text.ParseException;
import java.util.List;

/**
 * Created by weizhang on 1/19/17.
 */
@ManagedBean(name = ManagedBeanNames.pages_sbi_add_dataset)
@ViewScoped
public class SbiAddDatasetBean extends ASecureBean {
    private static final Logger logger = LogManager.getLogger(SbiBean.class);
    private NelsUser nelsUser;

    private long sbiProjectId;
    private Long sbiDatasetTypeId;
    private List<SbiDataSetType> sbiDataSetTypes;

    private SbiDataSetType sbiDataSetType;
    private String name;
    private String description;

    @Override
    public String getPageTitle() {
        if (!isPostback()) {
            secure();
            this.registerRequestUrl();
            UserSessionBean userSessionBean = JSFUtils.getManagedBean(ManagedBeanNames.session_userSessionBean, UserSessionBean.class);
            nelsUser = userSessionBean.getCurrentUser();
            try {
                String projectId = StringUtilities.DecryptSimple(URLParametersFacade.getURLParameter("projectId"), no.nels.portal.Config.getEncryptionSalt());
                sbiProjectId = Long.valueOf(projectId);
                String federatedId = nelsUser.getIdpUser().getIdpUsername();
                sbiDataSetTypes = SbiApiConsumer.getDataSetTypes(federatedId);
            } catch (ParseException | SbiException e) {
                logger.error(e.getLocalizedMessage());
            }

        }
        return "Sbi create dataset";

    }

    public Long getSbiDatasetTypeId() {
        return sbiDatasetTypeId;
    }

    public void setSbiDatasetTypeId(Long sbiDatasetTypeId) {
        this.sbiDatasetTypeId = sbiDatasetTypeId;
    }


    public boolean validateInput() {
        if (sbiDatasetTypeId == -1) {
            MessageFacade.AddError("Dataset type not selected");
            return false;
        }
        if (name.trim().equalsIgnoreCase("")) {
            MessageFacade.AddError("You must input the dataset name");
            return false;
        }
        if (!StringUtilities.isValidFileFolderName(name.trim())) {
            MessageFacade.invalidInput("Invalid character in dataset name. Modify your input and try again");
            return false;
        }
        List<SbiDataSet> dataSets = null;
        try {
            dataSets = SbiApiConsumer.getDataSets(nelsUser.getIdpUser().getEmail(), sbiProjectId);

        } catch (ParseException | SbiException e) {
            logger.error(e.getLocalizedMessage());
        }
        if (dataSets.stream().map(s -> s.getName()).anyMatch(n -> n.equalsIgnoreCase(name.trim()))) {
            MessageFacade.AddError("Dataset name already exists");
            return false;
        }
        if (description.trim().equalsIgnoreCase("")) {
            MessageFacade.AddError("You must input the description about the dataset");
            return false;
        }
        return true;
    }


    public void cmdCreateDataset_Click() {
        if (validateInput()) {
            logger.debug("user:" + nelsUser.getIdpUser().getEmail() + ",projectId:" + sbiProjectId + ",datsettypeId:" + sbiDatasetTypeId.longValue());
            boolean result = SbiApiConsumer.createDataSet(nelsUser.getIdpUser().getIdpUsername(), sbiProjectId, sbiDatasetTypeId.longValue(), name, description);
            if (result) {
                MessageFacade.addInfo("Success", "The new dataset added successfully", true);
                NavigationFacade.closePopup();
            } else {
                MessageFacade.AddError(
                        "Unable to add dataset",
                        "internal error");
            }
        }
    }

    public void cmdCancel_Click() {
        NavigationFacade.closePopup();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public NelsUser getNelsUser() {
        return nelsUser;
    }

    public void setNelsUser(NelsUser nelsUser) {
        this.nelsUser = nelsUser;
    }


    public List<SbiDataSetType> getSbiDataSetTypes() {
        return sbiDataSetTypes;
    }

    public void setSbiDataSetTypes(List<SbiDataSetType> sbiDataSetTypes) {
        this.sbiDataSetTypes = sbiDataSetTypes;
    }


    public SbiDataSetType getSbiDataSetType() {
        return sbiDataSetType;
    }

    public void setSbiDataSetType(SbiDataSetType sbiDataSetType) {
        this.sbiDataSetType = sbiDataSetType;
    }

    @Override
    public void secure() {

    }


}
