package no.nels.portal.model;

import no.nels.client.model.FileFolder;
import no.nels.commons.abstracts.AStringId;
import no.nels.commons.model.StringIndexedList;
import org.primefaces.model.SelectableDataModel;

import javax.faces.model.ListDataModel;
import java.util.List;

public class FileFolderGridModel extends ListDataModel<AStringId> implements
		SelectableDataModel<FileFolder> {

	StringIndexedList fileFolders = null;

	public FileFolderGridModel(StringIndexedList fileFolders) {
		super((List<AStringId>) fileFolders);
		this.fileFolders = fileFolders;
	}

	public FileFolder getRowData(String id) {
		return (FileFolder) fileFolders.getById(id);
	}

	public Object getRowKey(FileFolder fileFolder) {
		return fileFolder.getId();
	}

}
