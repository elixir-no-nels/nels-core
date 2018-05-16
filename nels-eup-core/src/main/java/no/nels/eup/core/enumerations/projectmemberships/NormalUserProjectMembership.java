package no.nels.eup.core.enumerations.projectmemberships;


import no.nels.eup.core.abstracts.AProjectMembership;

public class NormalUserProjectMembership extends AProjectMembership{
    @Override
    public long getId() {
        return 3;
    }
    public String toString() { return "Member";}
}
