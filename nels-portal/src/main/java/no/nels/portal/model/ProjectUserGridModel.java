package no.nels.portal.model;

import no.nels.commons.abstracts.ANumberId;
import no.nels.commons.model.NumberIndexedList;
import no.nels.commons.model.ProjectUser;
import org.primefaces.model.SelectableDataModel;

import javax.faces.model.ListDataModel;

public class ProjectUserGridModel extends ListDataModel<ANumberId> implements
        SelectableDataModel<ProjectUser> {
    private NumberIndexedList list;

    public ProjectUserGridModel(NumberIndexedList list) {
        super(list);
        this.list = list;
    }

    @Override
    public Object getRowKey(ProjectUser projectUser) {
        return projectUser.getId();
    }

    @Override
    public ProjectUser getRowData(String id) {
        return (ProjectUser) list.getById(Long.parseLong(id));
    }
}
