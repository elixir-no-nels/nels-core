package no.nels.portal.model;

import no.nels.commons.abstracts.ANumberId;
import no.nels.commons.model.NumberIndexedList;
import no.nels.idp.core.model.db.NeLSIdpUser;
import org.primefaces.model.SelectableDataModel;

import javax.faces.model.ListDataModel;

/**
 * Created by Kidane on 27.05.2015.
 */
public class IdpGridModel extends ListDataModel<ANumberId> implements SelectableDataModel<NeLSIdpUser>{
    private NumberIndexedList idps;

    public IdpGridModel(NumberIndexedList idps){
        super(idps);
        this.idps = idps;
    }

    public NeLSIdpUser getRowData(String id){return (NeLSIdpUser)idps.getById(Long.parseLong(id));}

    public Object getRowKey(NeLSIdpUser idp){return idp.getId();}
}
