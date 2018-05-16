package no.nels.api.sec;

/**
 * Created by xiaxi on 07/04/2017.
 */
@FunctionalInterface
public interface CheckedSupplier<T> {
    T get() throws Exception;
}
