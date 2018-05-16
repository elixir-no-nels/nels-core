package no.nels.commons.model;

import java.util.ArrayList;
import java.util.HashMap;

import no.nels.commons.abstracts.*;

public class StringIndexedList extends ArrayList<AStringId> {

	private static final long serialVersionUID = 1L;
	private HashMap<String, AStringId> index = new HashMap<String, AStringId>();

	@Override
	public boolean add(AStringId aString) {
		index.put(aString.getId(), aString);
		return super.add(aString);
	}

	@Override
	public boolean remove(Object aStringId) {
		if (aStringId instanceof AStringId) {
			if (index.containsKey(((AStringId) aStringId).getId())) {
				index.remove(((AStringId) aStringId).getId());
			}
		}
		return super.remove(aStringId);
	}

	public AStringId getById(String id) {
		return (index.containsKey(id)) ? index.get(id) : null;
	}

}
