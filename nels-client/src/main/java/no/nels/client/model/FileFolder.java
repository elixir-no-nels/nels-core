package no.nels.client.model;

import java.util.Date;

import no.nels.commons.abstracts.*;

public class FileFolder extends AStringId {
	private String name;
	private String path;
	private String description;
	private Date lastUpdate;
	private boolean isFolder;
	private long size;

	public FileFolder(String name, String path, String description,
			Date lastUpdate, long size, boolean isFolder) {
		this.name = name;
		this.path = path;
		this.description = description;
		this.lastUpdate = lastUpdate;
		this.size = size;
		this.isFolder = isFolder;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name.replace("/", "");
	}

	public boolean isFolder() {
		return isFolder;
	}

	public boolean isEmptyFolder() {
		return isFolder && (size == 0);
	}

	public void setFolder(boolean isFolder) {
		this.isFolder = isFolder;
	}

	// TODO: this should not be here . Should be handled by the view and not in
	// the model
	public String getCss() {
		String ret = "folder";
		if (!isFolder) {
			String extention = org.apache.commons.lang.StringUtils
					.substringAfterLast(name, ".");
			ret = "file " + extention;
		}
		return ret;
	}

	public String getDownloadCss(){
		return isFolder? "folder-download":"file-download";
	}

	@Override
	public String getId() {
		return (isFolder) ? "fldr-" + name : "fl-" + name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public Date getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(Date lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	public String getPath() {
		return path;
	}

	public String getParentFolder() {
		if (path.contains("/")) {
			return org.apache.commons.lang.StringUtils.substringBeforeLast(
					path, "/");
		}
		return null;
	}

	public void setPath(String path) {
		this.path = path;
	}
}
