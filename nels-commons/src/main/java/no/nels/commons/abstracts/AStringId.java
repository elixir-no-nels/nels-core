package no.nels.commons.abstracts;

public abstract class AStringId {
	public abstract String getId();

	@Override
	public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof AStringId) {
            return ((AStringId) obj).getId().equals(this.getId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.getId().hashCode();
    }
}
