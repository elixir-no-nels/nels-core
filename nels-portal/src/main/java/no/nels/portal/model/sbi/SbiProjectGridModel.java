package no.nels.portal.model.sbi;

import no.nels.client.sbi.models.SbiProject;
import no.nels.commons.abstracts.ANumberId;
import no.nels.commons.model.NumberIndexedList;
import org.primefaces.model.SelectableDataModel;

import javax.faces.model.ListDataModel;

public class SbiProjectGridModel extends ListDataModel<ANumberId> implements SelectableDataModel<SbiProject> {
    private NumberIndexedList list;

    public SbiProjectGridModel(NumberIndexedList list) {
        super(list);
        this.list = list;
    }

    @Override
    public Object getRowKey(SbiProject sbiProject) {
        return sbiProject.getId();
    }

    @Override
    public SbiProject getRowData(String id) {
        return (SbiProject) this.list.getById(Long.valueOf(id));
    }
}
