package no.nels.api.oauth;

import io.netty.handler.codec.http.HttpHeaders;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import no.nels.api.Config;
import no.nels.commons.constants.OAuthConstant;

import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.List;

public final class OAuthIntrospectionProvider implements AuthProvider{
    private static Logger logger = LoggerFactory.getLogger(OAuthIntrospectionProvider.class);

    private HttpClient httpClient;

    public OAuthIntrospectionProvider(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public void authenticate(JsonObject token, Handler<AsyncResult<User>> handler) {
        HttpClientRequest request = httpClient.post(Integer.valueOf(Config.valueOf(no.nels.commons.constants.ConfigName.OAUTH_PORT)), Config.valueOf(no.nels.commons.constants.ConfigName.OAUTH_HOST), "/oauth2/token", response -> {
            response.exceptionHandler(throwable -> handler.handle(Future.failedFuture(throwable)));
            response.bodyHandler(body -> {
                JsonObject responseBody = body.toJsonObject();
                String access_token = responseBody.getString(OAuthConstant.ACCESS_TOKEN);

                HttpClientRequest validateRequest = httpClient.post(Integer.valueOf(Config.valueOf(no.nels.commons.constants.ConfigName.OAUTH_PORT)), Config.valueOf(no.nels.commons.constants.ConfigName.OAUTH_HOST), "/oauth2/introspect", validateResponse -> {
                    validateResponse.exceptionHandler(throwable -> handler.handle(Future.failedFuture(throwable)));
                    validateResponse.bodyHandler(body1 -> {
                        JsonObject jsonObject = body1.toJsonObject();
                        Boolean active = jsonObject.getBoolean(OAuthConstant.ACTIVE);

                        if (active) {
                            if (jsonObject.getString(OAuthConstant.SCOPE).equalsIgnoreCase("component")) {
                                List<String> scopeList = Arrays.asList(jsonObject.getString(OAuthConstant.SCOPE).split(" "));
                                String name = jsonObject.getString(OAuthConstant.CLIENT_ID);
                                User authenticatedUser = new OAuthComponent(scopeList, name);
                                handler.handle(Future.succeededFuture(authenticatedUser));
                            } else {
                                int nelsId = jsonObject.getInteger(OAuthConstant.DOT_NELS_ID);
                                List<String> scopeList = Arrays.asList(jsonObject.getString(OAuthConstant.SCOPE).split(" "));
                                String federatedId = jsonObject.getString(OAuthConstant.DOT_FEDERATED_ID);
                                String name = jsonObject.getString(OAuthConstant.DOT_NAME);
                                String userType = jsonObject.getString(OAuthConstant.DOT_USER_TYPE);
                                User authenticatedUser = new OAuthUser(nelsId, scopeList, federatedId, name, userType);
                                handler.handle(Future.succeededFuture(authenticatedUser));
                            }

                        } else {
                            handler.handle(Future.failedFuture("Token not active"));
                        }
                    });
                });

                validateRequest.putHeader(HttpHeaders.Names.AUTHORIZATION, OAuthConstant.BEARER + " " + access_token);
                validateRequest.putHeader(HttpHeaders.Names.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
                validateRequest.end(OAuthConstant.TOKEN + "=" + token.getString(OAuthConstant.TOKEN));

            });
        });
        JsonObject requestBody = new JsonObject();
        requestBody.put(no.nels.commons.constants.ConfigName.CLIENT_ID, Config.valueOf(no.nels.commons.constants.ConfigName.CLIENT_ID))
                .put(no.nels.commons.constants.ConfigName.CLIENT_SECRET, Config.valueOf(no.nels.commons.constants.ConfigName.CLIENT_SECRET))
                .put(no.nels.commons.constants.ConfigName.GRANT_TYPE, Config.valueOf(no.nels.commons.constants.ConfigName.GRANT_TYPE))
                .put(no.nels.commons.constants.ConfigName.SCOPE, Config.valueOf(no.nels.commons.constants.ConfigName.SCOPE));
        request.putHeader(HttpHeaders.Names.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        request.end(requestBody.toBuffer());
    }
}
