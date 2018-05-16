package no.nels.portal.model.sbi;

import no.nels.client.sbi.models.SbiData;
import no.nels.commons.abstracts.ANumberId;
import no.nels.commons.model.NumberIndexedList;
import org.primefaces.model.SelectableDataModel;

import javax.faces.model.ListDataModel;

public class SbiFileGridModel extends ListDataModel<ANumberId> implements SelectableDataModel<SbiData> {
    private NumberIndexedList list;

    public SbiFileGridModel(NumberIndexedList list) {
        super(list);
        this.list = list;
    }

    @Override
    public Object getRowKey(SbiData sbiFile) {
        return sbiFile.getId();
    }

    @Override
    public SbiData getRowData(String id) {
        return (SbiData) list.getById(Long.valueOf(id));
    }

    public int size() {
        return list.size();
    }
}
