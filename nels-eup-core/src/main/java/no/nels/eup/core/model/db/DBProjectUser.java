package no.nels.eup.core.model.db;

import no.nels.commons.abstracts.*;

public class DBProjectUser extends ANumberId {
    private long id;
    private long projectId;
    private long userId;
    private long membershipType;

    @Override
    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getProjectId() {
        return projectId;
    }

    public void setProjectId(long projectId) {
        this.projectId = projectId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getMembershipType() {
        return membershipType;
    }

    public void setMembershipType(long membershipType) {
        this.membershipType = membershipType;
    }
}
