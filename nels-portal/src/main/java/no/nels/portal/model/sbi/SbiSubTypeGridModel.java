package no.nels.portal.model.sbi;
import no.nels.client.sbi.models.SbiSubtype;
import no.nels.commons.abstracts.ANumberId;
import no.nels.commons.model.NumberIndexedList;
import org.primefaces.model.SelectableDataModel;

import javax.faces.model.ListDataModel;

public class SbiSubTypeGridModel extends ListDataModel<ANumberId> implements SelectableDataModel<SbiSubtype> {
    private NumberIndexedList list;

    public SbiSubTypeGridModel(NumberIndexedList list) {
        super(list);
        this.list = list;
    }

    @Override
    public Object getRowKey(SbiSubtype sbiSubType) {
        return sbiSubType.getId();
    }

    @Override
    public SbiSubtype getRowData(String id) {
        return (SbiSubtype) this.list.getById(Long.valueOf(id));
    }
}
