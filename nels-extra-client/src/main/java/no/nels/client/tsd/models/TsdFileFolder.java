package no.nels.client.tsd.models;

import no.nels.commons.abstracts.AStringId;
import org.apache.commons.lang.StringUtils;

import java.nio.file.FileSystems;

public final class TsdFileFolder extends AStringId{

    private String name;
    private String path;
    private boolean isFolder;
    private long size;

    public TsdFileFolder(String name, String path, boolean isFolder, long size) {
        this.name = name;
        this.path = path;
        this.isFolder = isFolder;
        this.size = size;
    }

    @Override
    public String getId() {
        return StringUtils.join(new String[]{this.path, this.name}, FileSystems.getDefault().getSeparator());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isFolder() {
        return isFolder;
    }

    public void setIsFolder(boolean isFolder) {
        this.isFolder = isFolder;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
