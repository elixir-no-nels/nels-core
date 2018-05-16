package no.nels.portal.Brokers;

import no.nels.commons.model.NelsProject;

/**
 * Created by Kidane on 22.12.2015.
 */
public class ProjectBroker {
    public static String getStorageRoot(NelsProject project){
        //Caution: The project name in db might not be the same as path-name on the storage service.
        //Better to call a storage service method to get the mapping - when supported by the storage service
        return "Projects/" + project.getName();
    }
}
