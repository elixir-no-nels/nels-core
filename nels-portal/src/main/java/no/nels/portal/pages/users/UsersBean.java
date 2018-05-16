package no.nels.portal.pages.users;

import no.nels.client.UserApi;
import no.nels.commons.abstracts.ASystemUser;
import no.nels.commons.model.NelsUser;
import no.nels.commons.model.ProjectUser;
import no.nels.commons.model.projectmemberships.PIProjectMembership;
import no.nels.commons.model.systemusers.AdministratorUser;
import no.nels.commons.model.systemusers.HelpDeskUser;
import no.nels.commons.model.systemusers.NormalUser;
import no.nels.portal.abstracts.ASecureBean;
import no.nels.portal.facades.*;
import no.nels.portal.model.UserGridModel;
import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.model.enumerations.PageModes;
import no.nels.portal.model.enumerations.SessionItemKeys;
import no.nels.portal.model.enumerations.URLParameterNames;
import no.nels.portal.navigation.UserNavigationBean;
import no.nels.portal.session.UserSessionBean;
import no.nels.portal.utilities.Constants;
import no.nels.portal.utilities.JSFUtils;
import no.nels.portal.utilities.UserTypeUtilities;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import java.util.*;

@ManagedBean(name = ManagedBeanNames.pages_users_users)
@ViewScoped
public class UsersBean extends ASecureBean {

    UserGridModel users = new UserGridModel(UserApi.getAllNelsUsers());
    NelsUser[] selectedUsersForAction;

    public NelsUser[] getSelectedUsersForPick() {
        return selectedUsersForPick;
    }

    public void setSelectedUsersForPick(NelsUser[] selectedUsersForPick) {
        this.selectedUsersForPick = selectedUsersForPick;
    }

    NelsUser[] selectedUsersForPick;
    NelsUser selectedUser;
    long selectedSystemUserType;
    private String searchId;
    private String searchEmail;
    private String searchName;

    public String getSearchId() {
        return searchId;
    }

    public void setSearchId(String searchId) {
        this.searchId = searchId;
    }

    public String getSearchEmail() {
        return searchEmail;
    }

    public void setSearchEmail(String searchEmail) {
        this.searchEmail = searchEmail;
    }

    public String getSearchName() {
        return searchName;
    }

    public void setSearchName(String searchName) {
        this.searchName = searchName;
    }

    private List<SelectItem> systemUserTypeList = new ArrayList<SelectItem>() {
        {
            this.add(new SelectItem(new AdministratorUser().getId(), Constants.ADMINISTRATOR));
            this.add(new SelectItem(new HelpDeskUser().getId(), Constants.HELPDESK));
            this.add(new SelectItem(new NormalUser().getId(), Constants.NORMALUSER));
        }
    };

    public long getSelectedSystemUserType() {
        return selectedSystemUserType;
    }

    public void setSelectedSystemUserType(long selectedSystemUserType) {
        this.selectedSystemUserType = selectedSystemUserType;
    }

    public List<SelectItem> getSystemUserTypeList() {
        return systemUserTypeList;
    }

    public NelsUser[] getSelectedUsersForAction() {
        return selectedUsersForAction;
    }

    public void setSelectedUsersForAction(NelsUser[] selectedUsersForAction) {
        this.selectedUsersForAction = selectedUsersForAction;
    }

    public NelsUser getSelectedUser() {
        return selectedUser;
    }

    public void setSelectedUser(NelsUser selectedUser) {
        this.selectedUser = selectedUser;
    }

    public UserGridModel getUsers() {
        return users;
    }

    public void secure() {
        if (!isPi()) {
            ArrayList<ASystemUser> userTypes = new ArrayList<ASystemUser>() {
                {
                    add(new AdministratorUser());
                    add(new HelpDeskUser());
                }
            };
            SecurityFacade.requireSystemUserType(userTypes);
        }
    }

    public boolean isAdminOrHelpdesk() {
        return SecurityFacade.isUserAdmin() || SecurityFacade.isUserHelpDesk();
    }

    public boolean isPi() {
        UserSessionBean userSessionBean = JSFUtils.getManagedBean(ManagedBeanNames.session_userSessionBean, UserSessionBean.class);
        HashMap<String, ProjectUser> map = (HashMap<String, ProjectUser>) SessionFacade.getSessionObject(SessionItemKeys.PROJECTS_OF_USER, userSessionBean.getCurrentUser().getId());
        if (map == null) {
            return false;
        } else {
            for (Map.Entry<String, ProjectUser> entry : map.entrySet()) {
                if (entry.getValue().getMembership().equals(new PIProjectMembership())) {
                    return true;
                }
            }
            return false;
        }
    }

    public String getPageTitle() {
        LoggingFacade.logDebugInfo("getting title for users page");
        if (!isPostback()) {
            secure();
            this.registerRequestUrl();
            if (this.getPageMode().equalsIgnoreCase(PageModes.PickOne)
                    || this.getPageMode().equalsIgnoreCase(
                    PageModes.PickMultiple)) {
                URLParametersFacade
                        .requireURLParameter(URLParameterNames.PickerCallKey);
            }
        }
        return "users";
    }

    public void reloadUsers() {
        if (searchId == null || searchName == null) {
            users = new UserGridModel(UserApi.searchNeLSUsers(-1, searchEmail.trim(), ""));
        } else {
            long id = -1;
            try {
                id = Long.parseLong(this.searchId);
            } catch (Exception ex) {
                searchId = "";
            }
            users = new UserGridModel(UserApi.searchNeLSUsers(id, searchEmail.trim(), searchName.trim()));
            //users = new UserGridModel(UserFacade.getAllNelsUsers());
        }
    }

    public void viewUser(long userId) {
        JSFUtils.getManagedBean(ManagedBeanNames.navigation_userBean,
                UserNavigationBean.class).viewUserDetail(userId);
    }

    public void updateUser() {
        if (selectedUsersForAction == null) {
            MessageFacade.noRowsSelected();
            return;
        }
        if (selectedUsersForAction.length == 0) {
            MessageFacade.noRowsSelected();
            return;
        }

        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        String actionValue = params.get("action");

        boolean isSuccess = false;
        String message = "";
        LoggingFacade.logDebugInfo("The action value is " + actionValue);
        if (actionValue.equals("modifyUsertype")) {
            isSuccess = UserApi.modifySystemUserType(Arrays.asList(selectedUsersForAction), new ASystemUser() {
                @Override
                public long getId() {
                    return selectedSystemUserType;
                }
            });
            message = "Selected users are modified ";
        } else if (actionValue.equals("activate")) {
            isSuccess = UserApi.activateNelsUser(Arrays.asList(selectedUsersForAction));
            message = "Selected users are activated ";
        } else if (actionValue.equals("deactivate")) {
            isSuccess = UserApi.deactivateNelsUser(Arrays.asList(selectedUsersForAction));
            message = "Selected users are deactivated ";
        }

        if (isSuccess) {
            MessageFacade.addInfo(message.concat("successfully"), "");
            selectedUsersForAction = null;
            reloadUsers();
        } else {
            MessageFacade.AddError(message.concat("unsuccessfully"), "Internal error");
        }

    }

    public String getSystemUserTypeName(long id) {
        return UserTypeUtilities.getUserTypeName(no.nels.commons.utilities.UserTypeUtilities.getUserType(id));
    }

    public void cmdAcceptPicker_Click() {
        if (this.getPageMode().equalsIgnoreCase(PageModes.PickMultiple)) {
            if (selectedUsersForPick.length > 0) {
                PickerFacade.returnFromPicker(this, selectedUsersForPick);
            } else {
                MessageFacade.noRowsSelected();
            }
        }
    }

    public void cmdCancelPicker_Click() {
        NavigationFacade.closePopup();
    }

    public void cmdSearch_Click() {
        reloadUsers();
    }

    public void cmdRefreshSearch_Click() {
        this.searchId = "";
        this.searchEmail = "";
        this.searchName = "";
        reloadUsers();
    }
}
