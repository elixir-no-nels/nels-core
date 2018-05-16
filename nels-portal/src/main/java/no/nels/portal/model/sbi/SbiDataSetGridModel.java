package no.nels.portal.model.sbi;

import no.nels.client.sbi.models.SbiDataSet;
import no.nels.commons.abstracts.ANumberId;
import no.nels.commons.model.NumberIndexedList;
import org.primefaces.model.SelectableDataModel;

import javax.faces.model.ListDataModel;

public class SbiDataSetGridModel extends ListDataModel<ANumberId> implements SelectableDataModel<SbiDataSet> {
    private NumberIndexedList list;

    public SbiDataSetGridModel(NumberIndexedList list) {
        super(list);
        this.list = list;
    }

    @Override
    public Object getRowKey(SbiDataSet sbiDataSet) {
        return sbiDataSet.getId();
    }

    @Override
    public SbiDataSet getRowData(String id) {
        return (SbiDataSet) this.list.getById(Long.valueOf(id));
    }


    public SbiDataSet getRowDataByName(String name) {
        
        return null;
    }
}
