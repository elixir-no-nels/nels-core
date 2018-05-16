package no.norstore.storebioinfo.utils;

import java.util.LinkedList;
import java.util.List;

public class ArchiveEntry {
    private String name;
    private long size = 0L;
    private String type = "";
    private String url = "";
    private String storagePath = "";

    private List<String> subEntries = null;

    private String id = "";

    public List<String> getSubEntries() {
	return subEntries;
    }

    public ArchiveEntry(String name, long size, String type, String url) {
	super();
	this.name = name;
	this.size = size;
	this.type = type;
	this.url = url;
	this.subEntries = new LinkedList<String>();
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

    public String getType() {
	return type;
    }

    public void setType(String type) {
	this.type = type;
    }

    public String getUrl() {
	return url;
    }

    public void setUrl(String url) {
	this.url = url;
    }

    public String getId() {
	return id;
    }

    public void setId(String id) {
	this.id = id;
    }

    public String getStoragePath() {
	return storagePath;
    }

    public void setStoragePath(String storagePath) {
	this.storagePath = storagePath;
    }

}