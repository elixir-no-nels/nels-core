package no.nels.portal.model;

import java.util.List;

import javax.faces.model.ListDataModel;

import no.nels.commons.model.NelsUser;
import no.nels.commons.model.NumberIndexedList;
import org.primefaces.model.SelectableDataModel;

import no.nels.commons.abstracts.ANumberId;

public class UserGridModel extends ListDataModel<ANumberId> implements
		SelectableDataModel<NelsUser> {

	NumberIndexedList users = null;

	public UserGridModel(NumberIndexedList users) {
		super((List<ANumberId>) users);
		this.users = users;
	}

	public NelsUser getRowData(String id) {
		return (NelsUser) users.getById(Long.parseLong(id));
	}

	public Object getRowKey(NelsUser usr) {
		return usr.getId();
	}

}
