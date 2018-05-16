package no.nels.api.oauth;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AbstractUser;
import io.vertx.ext.auth.AuthProvider;
import no.nels.api.constants.JsonKey;

import java.util.Arrays;
import java.util.List;

public final class OAuthUser extends AbstractUser{
    private int nelsId;
    private List<String> scope;
    private String federatedId;
    private String name;
    private String userType;
    private AuthProvider authProvider;

    public OAuthUser(int nelsId, List<String> scope, String federatedId, String name, String userType) {
        this.nelsId = nelsId;
        this.scope = scope;
        this.federatedId = federatedId;
        this.name = name;
        this.userType = userType;
    }

    @Override
    public JsonObject principal() {
        return new JsonObject().put(JsonKey.NELS_ID, this.nelsId)
                .put(JsonKey.SCOPE, new JsonArray(this.scope))
                .put(JsonKey.FEDERATED_ID, this.federatedId)
                .put(JsonKey.NAME, this.name)
                .put(JsonKey.USER_TYPE, this.userType);
    }

    @Override
    public void setAuthProvider(AuthProvider authProvider) {
        this.authProvider = authProvider;
    }

    public AuthProvider getAuthProvider() {
        return this.authProvider;
    }

    @Override
    protected void doIsPermitted(String s, Handler<AsyncResult<Boolean>> handler) {

    }
}
