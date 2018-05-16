package no.nels.portal.model;

import no.nels.commons.abstracts.ANumberId;
import no.nels.commons.model.NelsProject;
import no.nels.commons.model.NumberIndexedList;
import org.primefaces.model.SelectableDataModel;

import javax.faces.model.ListDataModel;

public class ProjectGridModel extends ListDataModel<ANumberId> implements SelectableDataModel<NelsProject> {
    private NumberIndexedList projects;

    public ProjectGridModel(NumberIndexedList projects) {
        super(projects);
        this.projects = projects;
    }

    public NelsProject getRowData(String id) {
        return (NelsProject) projects.getById(Long.parseLong(id));
    }

    public Object getRowKey(NelsProject project) {
        return project.getId();
    }
}
