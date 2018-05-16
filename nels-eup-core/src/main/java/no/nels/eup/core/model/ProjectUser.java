package no.nels.eup.core.model;

import no.nels.commons.abstracts.*;
import no.nels.commons.model.NelsUser;
import no.nels.eup.core.abstracts.AProjectMembership;

public class ProjectUser extends ANumberId {
    private long id = -1;
    private NelsUser user;
    private NelsProject project;
    private AProjectMembership membership;

    public ProjectUser() {}

    public ProjectUser(long id, NelsUser user, NelsProject project, AProjectMembership membership) {
        this.id = id;
        this.user = user;
        this.project = project;
        this.membership = membership;
    }

    @Override
    public long getId() {
        return id;
    }

    public NelsUser getUser() {
        return user;
    }

    public NelsProject getProject() {
        return project;
    }

    public AProjectMembership getMembership() {
        return membership;
    }
}
