package no.nels.master.api;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AbstractUser;
import io.vertx.ext.auth.AuthProvider;

public final class BasicHttpUser extends AbstractUser {
    public final static String IS_PERMITTED = "permitted";

    private final JsonObject principal;

    public BasicHttpUser(JsonObject principal, boolean isPermitted) {
        this.principal = new JsonObject();
        this.principal.mergeIn(principal).put(BasicHttpUser.IS_PERMITTED, isPermitted);
    }

    @Override
    public JsonObject principal() {
        return this.principal;
    }

    @Override
    public void setAuthProvider(AuthProvider authProvider) {

    }

    @Override
    protected void doIsPermitted(String s, Handler<AsyncResult<Boolean>> handler) {

    }
}
