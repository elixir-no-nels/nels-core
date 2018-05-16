package no.nels.portal.model.tsd;

import no.nels.client.tsd.models.TsdFileFolder;
import no.nels.commons.abstracts.AStringId;
import no.nels.commons.model.StringIndexedList;
import org.primefaces.model.SelectableDataModel;

import javax.faces.model.ListDataModel;

public final class TsdFileGridModel extends ListDataModel<AStringId> implements SelectableDataModel<TsdFileFolder> {

    private StringIndexedList tsdFileFolder;

    public TsdFileGridModel(StringIndexedList tsdFileFolder) {
        super(tsdFileFolder);
        this.tsdFileFolder = tsdFileFolder;
    }

    @Override
    public Object getRowKey(TsdFileFolder tsdFileFolder) {
        return tsdFileFolder.getId();
    }

    @Override
    public TsdFileFolder getRowData(String id) {
        return (TsdFileFolder) tsdFileFolder.getById(id);
    }
}
