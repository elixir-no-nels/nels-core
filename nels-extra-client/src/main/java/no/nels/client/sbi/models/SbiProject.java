package no.nels.client.sbi.models;

import no.nels.commons.abstracts.ANumberId;

import java.util.Date;

public final class SbiProject extends ANumberId {
    private long id;
    private String name;
    private Date creationDate;
    private String contactPerson;
    private String description;
    private String contactEmail;
    private String contactAffiliation;
    private String quotaName;
    private long quotaSize;
    private long usedSize;
    private long diskUsage;

    public SbiProject(long id, String name, String contactPerson, String description, Date creationDate) {
        this.id = id;
        this.name = name;
        this.contactPerson = contactPerson;
        this.description = description;
        this.creationDate = creationDate;
    }

    public SbiProject(long id, String name, String contactPerson, String description, String contactEmail,
                      String contactAffiliation, Date creationDate, String quotaName, long quotaSize, long usedSize, long diskUsage) {
        this.id = id;
        this.name = name;
        this.contactPerson = contactPerson;
        this.description = description;
        this.creationDate = creationDate;
        this.contactEmail = contactEmail;
        this.contactAffiliation = contactAffiliation;
        this.quotaName = quotaName;
        this.quotaSize = quotaSize;
        this.usedSize = usedSize;
        this.diskUsage = diskUsage;
    }

    @Override
    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public String getDescription() {
        return description;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public String getContactAffiliation() {
        return contactAffiliation;
    }

    public String getQuotaName() {
        return quotaName;
    }

    public long getQuotaSize() {
        return quotaSize;
    }

    public long getUsedSize() {
        return usedSize;
    }

    public long getDiskUsage() {
        return diskUsage;
    }
}
