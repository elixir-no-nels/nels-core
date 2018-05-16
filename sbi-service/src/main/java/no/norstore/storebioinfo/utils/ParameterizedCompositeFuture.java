package no.norstore.storebioinfo.utils;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.impl.CompositeFutureImpl;

import java.util.List;

public interface ParameterizedCompositeFuture extends CompositeFuture {
    static <T> CompositeFuture join(List<Future<T>> futures) {
        return CompositeFutureImpl.join(futures.toArray(new Future[futures.size()]));
    }
}
