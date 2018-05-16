package no.nels.portal.model;

import no.nels.client.model.FileFolder;
import no.nels.portal.model.enumerations.SelectionPurpose;

import java.util.HashMap;

public class FileFolderSelection {
	private HashMap<String, FileFolder> selectedItems = null;
	private SelectionPurpose purpose = SelectionPurpose.COPY;
	private String selectedItemsRootFolder = "";
	private boolean havingFolders = false;

	public FileFolderSelection(String selectedItemsRootFolder,
			FileFolder[] items, SelectionPurpose purpose) {
		this.selectedItems = new HashMap<String, FileFolder>();
		for (FileFolder item : items) {
			this.selectedItems.put(item.getPath(), item);
			if (item.isFolder()) {
				havingFolders = true;
			}
		}
		this.purpose = purpose;
		this.selectedItemsRootFolder = selectedItemsRootFolder;
	}

	public HashMap<String, FileFolder> getSelectedItems() {
		return selectedItems;
	}

	public SelectionPurpose getPurpose() {
		return purpose;
	}

	public String getSelectedItemsRootFolder() {
		return selectedItemsRootFolder;
	}

	public boolean isHavingFolder() {
		return havingFolders;
	}

}
