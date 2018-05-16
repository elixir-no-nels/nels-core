package no.nels.commons.model;

import java.util.ArrayList;
import java.util.HashMap;

import no.nels.commons.abstracts.*;

public class NumberIndexedList extends ArrayList<ANumberId> {

	private static final long serialVersionUID = 1L;
	private HashMap<Long, ANumberId> index = new HashMap<Long, ANumberId>();

	@Override
	public boolean add(ANumberId aid) {
		index.put(aid.getId(), aid);
		return super.add(aid);
	}

	@Override
	public boolean remove(Object aid) {
		if (aid instanceof ANumberId) {
			if (index.containsKey(((ANumberId) aid).getId())) {
				index.remove(((ANumberId) aid).getId());
			}
		}
		return super.remove(aid);
	}

	public ANumberId getById(long id) {
		return (index.containsKey(id)) ? index.get(id) : null;
	}

	@Override
	public int size() {
		return index.size();
	}
	
	
	

}
