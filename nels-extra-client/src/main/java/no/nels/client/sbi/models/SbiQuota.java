package no.nels.client.sbi.models;

import java.util.Date;

public final class SbiQuota {
    private long id;
    private String name;
    private Date creationDate;
    private String description;
    private long quotaSize;
    private long usedSize;
    private long quotaId;

    public SbiQuota(long id, String name, Date creationDate, String description, long quotaSize, long usedSize, long quotaId) {
        this.id = id;
        this.name = name;
        this.creationDate = creationDate;
        this.description = description;
        this.quotaSize = quotaSize;
        this.usedSize = usedSize;
        this.quotaId = quotaId;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public String getDescription() {
        return description;
    }

    public long getQuotaSize() {
        return quotaSize;
    }

    public long getQuotaId() {
        return quotaId;
    }

    public long getUsedSize() {
        return usedSize;
    }
}
