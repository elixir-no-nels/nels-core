package no.nels.commons.abstracts;

public abstract class ANumberId {
	public abstract long getId();

	@Override
	public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ANumberId) {
            return ((ANumberId) obj).getId() == this.getId();
        }
        return false;
    }
	/*
    @Override
    public int hashCode() {
        return Long.hashCode(this.getId());
    }
    */
}
