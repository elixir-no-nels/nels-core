package no.nels.client.sbi.models;

import no.nels.commons.abstracts.ANumberId;


public final class SbiData extends ANumberId {
    private long id;
    private String name;
    private long size;
    private boolean isFolder;
    private String parentFolder;

    public SbiData(long id, String name, long size, boolean isFolder, String parentFolder) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.isFolder = isFolder;
        this.parentFolder = parentFolder;
    }

    @Override
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public boolean isFolder() {
        return isFolder;
    }

    public void setFolder(boolean folder) {
        isFolder = folder;
    }

    public String getParentFolder() {
        return parentFolder;
    }

    public void setParentFolder(String parentFolder) {
        this.parentFolder = parentFolder;
    }
}
