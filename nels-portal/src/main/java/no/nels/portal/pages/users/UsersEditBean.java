package no.nels.portal.pages.users;

import no.nels.client.UserApi;
import no.nels.commons.abstracts.ASystemUser;
import no.nels.commons.model.NelsUser;
import no.nels.portal.abstracts.ASecureBean;
import no.nels.portal.facades.*;
import no.nels.portal.model.UserGridModel;
import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.model.enumerations.PageModes;
import no.nels.portal.model.enumerations.URLParameterNames;
import no.nels.portal.navigation.UserNavigationBean;
import no.nels.portal.utilities.JSFUtils;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@ManagedBean(name = ManagedBeanNames.pages_users_users_edit)
@ViewScoped
public class UsersEditBean extends ASecureBean {

	UserGridModel users = new UserGridModel(UserApi.getAllNelsUsers());
	NelsUser[] selectedUsers;
	NelsUser selectedUser;
	
	long selectSystemUserType; 
	
	//String action;
	List<SelectItem> systemUserTypeList;

	private static List<SelectItem> createSystemUserTypeList() {
		return null;
	}

	public String getSystemUserTypeName(long id) {
		return null;
	}
	
    public List<SelectItem> getSystemUserTypeList() {
        return new ArrayList<SelectItem>() {
            {
                this.add(new SelectItem(1, "Administrator"));
                this.add(new SelectItem(2, "Help Desk"));
                this.add(new SelectItem(3, "Normal User"));
            }
        };
    }  
    
	public NelsUser[] getSelectedUsers() {
		return selectedUsers;
	}

	public void setSelectedUsers(NelsUser[] selectedUsers) {
		this.selectedUsers = selectedUsers;
	}

	public NelsUser getSelectedUser() {
		return selectedUser;
	}

	public void setSelectedUser(NelsUser selectedUser) {
		this.selectedUser = selectedUser;
	}

	public void secure() {
		//TODO: write security code here 
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
		return "Users edit";
	}

	public UserGridModel getUsers() {
		return users;
	}

	public void reloadUsers() {
		users = new UserGridModel(UserApi.getAllNelsUsers());
	}

	
	public void updateUser() {
		LoggingFacade.logDebugInfo("Update user action");
        if (this.selectedUsers != null && this.selectedUsers.length != 0) {
            Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
            String actionValue = params.get("action");

            boolean isSuccess = false;
            String message = "";
            LoggingFacade.logDebugInfo("The action value is " + actionValue);
            if (actionValue.equals("modifyUsertype")) {
                isSuccess = UserApi.modifySystemUserType(Arrays.asList(selectedUsers), new ASystemUser() {
					@Override
					public long getId() {
						return selectSystemUserType;
					}
				});
                message = "Selected users are modified ";
            } else if (actionValue.equals("activate")) {
                isSuccess = UserApi.activateNelsUser(Arrays.asList(selectedUsers));
                message = "Selected users are activated ";
            } else if (actionValue.equals("deactivate")) {
                isSuccess = UserApi.deactivateNelsUser(Arrays.asList(selectedUsers));
                message = "Selected users are deactivated ";
            }

            if (isSuccess) {
                MessageFacade.addInfo(message.concat("successfully"), "");
                selectedUsers = null;
                reloadUsers();
            } else {
                MessageFacade.AddError(message.concat("unsuccessfully"), "Internal error");
            }
        } else {
            MessageFacade.noRowsSelected();
        }

        //TODO: Rewrite this function using cleaned up core classes
    }

    public void viewUser(long userId) {
		JSFUtils.getManagedBean(ManagedBeanNames.navigation_userBean,
				UserNavigationBean.class).viewUserDetail(userId);
	}
	
	public void browseUser(long userId){
		SecurityFacade.setUserBeingViewed(userId);
		NavigationFacade.goHome();
	}

	public void cmdAcceptPicker_Click() {
		if (this.getPageMode().equalsIgnoreCase(PageModes.PickMultiple)) {
			if (selectedUsers.length > 0) {
				PickerFacade.returnFromPicker(this, selectedUsers);
			} else {
				MessageFacade.noRowsSelected();
			}
		} else if (this.getPageMode().equalsIgnoreCase(PageModes.PickOne)) {
			if (selectedUser != null) {
				PickerFacade.returnFromPicker(this,
						new Object[] { selectedUser });
			} else {
				MessageFacade.noRowsSelected();
			}
		}
	}

	public void cmdCancelPicker_Click() {
		NavigationFacade.closePopup();
	}

	public long getSelectSystemUserType() {
		return selectSystemUserType;
	}

	public void setSelectSystemUserType(long selectSystemUserType) {
		this.selectSystemUserType = selectSystemUserType;
	}
	
	
}
