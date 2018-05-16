package no.nels.eup.core.facades;


import no.nels.commons.model.IDPUser;
import no.nels.commons.model.NelsUser;
import no.nels.commons.model.NumberIndexedList;
import no.nels.commons.utilities.IDpUtilities;
import no.nels.commons.utilities.StringUtilities;
import no.nels.commons.utilities.UserTypeUtilities;
import no.nels.eup.core.Config;
import no.nels.eup.core.abstracts.AProjectMembership;
import no.nels.eup.core.model.NelsProject;
import no.nels.eup.core.model.ProjectUser;
import no.nels.eup.core.model.db.*;
import no.nels.eup.core.utilities.ProjectMembershipTypeUtilities;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public final class ProjectFacade {

    public static long getProjectsCount() {
        return Config.getEUPDBhelper().getCount("project");
    }

    public static long getMembersCountInProject(long projectId) {
        String cmd = "select count(*) from project_users where project_id=?";
        return Config.getEUPDBhelper().executeScalar(cmd, new Object[]{projectId});
    }

    public static boolean updateProject(NelsProject newProject) {
        String cmd = "update project set name=?, description=? where id=?";
        return Config.getEUPDBhelper().executeNonQuery(cmd, new Object[]{newProject.getName(), newProject.getDescription(), newProject.getId()});
    }

    public static NelsProject getProject(long id) {
        String cmd = "select * from project where id=? limit 1";
        DBProject project = Config.getEUPDBhelper().executeQueryForSingleResult(cmd, new Object[]{id}, new DBProjectMapper());
        NelsProject ret = new NelsProject(project.getId(), project.getName(), project.getDescription(), project.getCreationDate());
        return ret;
    }

    public static NelsProject getProjectByName(String name) {
        String cmd = "select * from project where name=? limit 1";
        DBProject project = Config.getEUPDBhelper().executeQueryForSingleResult(cmd, new Object[]{name}, new DBProjectMapper());
        NelsProject ret = new NelsProject(project.getId(), project.getName(), project.getDescription(), project.getCreationDate());
        return ret;
    }

    public static NumberIndexedList getAllProjects() {
        NumberIndexedList ret = new NumberIndexedList();
        String cmd = "select * from project";
        List<DBProject> projects = Config.getEUPDBhelper().executeQuery(cmd, new DBProjectMapper());
        for (DBProject project : projects) {
            ret.add(new NelsProject(project.getId(), project.getName(), project.getDescription(), project.getCreationDate()));
        }
        return ret;
    }


    public static NumberIndexedList searchProjects(long id, String namePartial) {
        //caution - no check is done for SQL Injection on string filter input parameters
        NumberIndexedList ret = new NumberIndexedList();
        String cmd = "SELECT * from project ";
        //construct filter
        String where = "";
        if (id != -1) {
            where = StringUtilities.AppendAndString(where, "id = " + id);
        }
        if (!namePartial.equals("")) {
            where = StringUtilities.AppendAndString(where, "LOWER(name) LIKE LOWER('%" + namePartial + "%')");
        }
        if (!where.equals("")) {
            cmd += " WHERE " + where;
        }
        //do the search
        for (DBProject project : Config.getEUPDBhelper().executeQuery(cmd, new DBProjectMapper())) {
            ret.add(new NelsProject(project.getId(), project.getName(), project.getDescription(), project.getCreationDate()));
        }
        return ret;
    }


    public static boolean isProjectNameExisting(String name) {
        String cmd = "select count(*) from project where name=?";
        return (Config.getEUPDBhelper().executeScalar(cmd, new Object[]{name}) == 1);

    }

    public static NelsProject addNewProject(String name, String description) {
        String cmd = "insert into project (name, description, creation_date) values (?, ?, ?)";
        try {
            boolean dbAdded = Config.getEUPDBhelper().executeNonQuery(cmd, new Object[]{name, description, new Date()});
            if (dbAdded) {
                return getProjectByName(name);

            }
        } catch (Exception ex) {

        }
        return null;

    }

    public static boolean deleteProject(List<Long> projectIds) {
        String cmd = "delete from project where id in (" + StringUtils.join(projectIds, ",") + ")";
        return Config.getEUPDBhelper().executeNonQuery(cmd);
    }

    public static boolean addUserToProject(List<Long> userIds, List<Long> projectIds, AProjectMembership membership) {
        List<String> value = new ArrayList<String>();
        for (long projectId : projectIds) {
            for (long userId : userIds) {
                value.add("(" + projectId + "," + userId + "," + membership.getId() + ")");
            }
        }
        String cmd = "insert into project_users (project_id, user_id, membership_type) values " + StringUtils.join(value, ",");
        return Config.getEUPDBhelper().executeNonQuery(cmd);
    }

    public static boolean removeUserFromProject(List<Long> userIds, List<Long> projectIds) {
        String cmd = "delete from project_users where user_id in (" + StringUtils.join(userIds, ",") + ") and project_id in (" + StringUtils.join(projectIds, ",") + ")";
        return Config.getEUPDBhelper().executeNonQuery(cmd);
    }

    public static boolean changeProjectMembership(List<Long> userIds, List<Long> projectIds, AProjectMembership newMembership) {
        String cmd = "update project_users set membership_type=? where user_id in (" + StringUtils.join(userIds, ",") + ") and project_id in (" + StringUtils.join(projectIds, ",") + ")";
        return Config.getEUPDBhelper().executeNonQuery(cmd, new Object[]{newMembership.getId()});
    }

    public static List<Long> getPojectIdListForUser(long userId) {
        String cmd = "select project_id from project_users where user_id=?";
        return Config.getEUPDBhelper().executeQueryBySingleColumn(cmd, new Object[]{userId}, "project_id", Long.class);
    }

    public static List<Long> getUserIdListForProject(long projectId) {
        String cmd = "select user_id from project_users where project_id=?";
        return Config.getEUPDBhelper().executeQueryBySingleColumn(cmd, new Object[]{projectId}, "user_id", Long.class);
    }

    public static NumberIndexedList getMembersInProject(long projectId) {
        String cmd = "select * from project_users where project_id=?";
        List<DBProjectUser> projectUserList = Config.getEUPDBhelper().executeQuery(cmd, new Object[]{projectId}, new DBProjectUserMapper());
        String userIds = "";
        Map<Long, DBProjectUser> userMap = new HashMap<Long, DBProjectUser>(projectUserList.size());
        for (DBProjectUser projectUser : projectUserList) {
            userIds = StringUtilities.AppendWithDelimiter(userIds, Long.toString(projectUser.getUserId()), ",");
            userMap.put(projectUser.getUserId(), projectUser);
        }
        NumberIndexedList ret = new NumberIndexedList();
        if (projectUserList.size() != 0) {
            cmd = "select * from users where id in (" + userIds + ")";
            List<DBUser> userList = Config.getEUPDBhelper().executeQuery(cmd, new DBUserMapper());
            for (DBUser user : userList) {
                ret.add(new ProjectUser(userMap.get(user.getId()).getId(),
                        new NelsUser(new IDPUser(IDpUtilities.getIDp(user.getIdpNumber()), user.getIdpUsername(), user.getName(), user.getEmail(), user.getAffiliation()), user.getId(), UserTypeUtilities.getUserType(user.getUserType()), user.isActive()),
                        null,
                        ProjectMembershipTypeUtilities.getProjectMembership(userMap.get(user.getId()).getMembershipType())));
            }
        }
        return ret;
    }

    public static AProjectMembership getProjectMembershipType(long projectId, long userId) {
        String cmd = "Select * from project_users where project_id=? and user_id=?";
        List<DBProjectUser> memberships = Config.getEUPDBhelper().executeQuery(cmd, new Object[]{projectId, userId}, new DBProjectUserMapper());
        //assume only one role per project
        return memberships.size() == 1 ? ProjectMembershipTypeUtilities.getProjectMembership(memberships.get(0).getMembershipType()) : null;
    }

    public static NumberIndexedList getProjectsForUser(long userId) {
        String cmd = "select * from project_users where user_id=?";
        List<DBProjectUser> projectUserList = Config.getEUPDBhelper().executeQuery(cmd, new Object[]{userId}, new DBProjectUserMapper());
        String projectIds = "";
        Map<Long, DBProjectUser> projectMap = new HashMap<Long, DBProjectUser>(projectUserList.size());
        for (DBProjectUser projectUser : projectUserList) {
            projectIds = StringUtilities.AppendWithDelimiter(projectIds, Long.toString(projectUser.getProjectId()), ",");
            projectMap.put(projectUser.getProjectId(), projectUser);
        }
        NumberIndexedList ret = new NumberIndexedList();
        if (projectUserList.size() != 0) {
            cmd = "select * from project where id in (" + projectIds + ") order by name";
            List<DBProject> projectList = Config.getEUPDBhelper().executeQuery(cmd, new DBProjectMapper());
            for (DBProject project : projectList) {
                ret.add(new ProjectUser(projectMap.get(project.getId()).getId(),
                        null,
                        new NelsProject(project.getId(), project.getName(), project.getDescription(), project.getCreationDate()),
                        ProjectMembershipTypeUtilities.getProjectMembership(projectMap.get(project.getId()).getMembershipType())));
            }
        }
        return ret;
    }

}
