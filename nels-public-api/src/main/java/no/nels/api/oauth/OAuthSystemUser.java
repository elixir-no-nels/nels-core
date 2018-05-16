package no.nels.api.oauth;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AbstractUser;
import io.vertx.ext.auth.AuthProvider;
import no.nels.api.constants.JsonKey;

import java.util.List;

public final class OAuthSystemUser extends AbstractUser {
    private AuthProvider authProvider;
    private List<String> scope;
    private String name;

    public OAuthSystemUser(List<String> scope, String name) {
        this.scope = scope;
        this.name = name;
    }

    @Override
    public JsonObject principal() {
        return new JsonObject().put(JsonKey.SCOPE, new JsonArray(this.scope)).put(JsonKey.NAME, this.name);
    }

    @Override
    public void setAuthProvider(AuthProvider authProvider) {
        this.authProvider = authProvider;
    }

    @Override
    protected void doIsPermitted(String s, Handler<AsyncResult<Boolean>> handler) {
    }

    public AuthProvider getAuthProvider() {
        return this.authProvider;
    }
}
