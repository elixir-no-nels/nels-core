package no.norstore.storebioinfo.utils;

/**
 * Created by xiaxi on 10/07/2017.
 */
@FunctionalInterface
public interface CheckedAction {
    void execute() throws Exception;
}
