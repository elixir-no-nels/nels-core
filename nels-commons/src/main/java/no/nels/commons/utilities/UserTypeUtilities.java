package no.nels.commons.utilities;

import no.nels.commons.abstracts.ASystemUser;
import no.nels.commons.model.systemusers.AdministratorUser;
import no.nels.commons.model.systemusers.HelpDeskUser;
import no.nels.commons.model.systemusers.NormalUser;

public final class UserTypeUtilities {
    public static ASystemUser getUserType(long id) {
        if(new AdministratorUser().getId() == id){
            return new AdministratorUser();
        }
        else if(new HelpDeskUser().getId() == id){
            return new HelpDeskUser();
        }
        else if(new NormalUser().getId() == id){
            return new NormalUser();
        }
        return null;
    }
}
