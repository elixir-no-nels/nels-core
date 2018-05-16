package no.nels.api.sec;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.impl.AuthHandlerImpl;
import no.nels.commons.constants.OAuthConstant;

public final class BearerAuthHandler extends AuthHandlerImpl {
    private static Logger logger = LoggerFactory.getLogger(BearerAuthHandler.class);

    public BearerAuthHandler(AuthProvider authProvider) {
        super(authProvider);
    }

    @Override
    public void handle(RoutingContext context) {
        HttpServerRequest request = context.request();
        if (request.path().startsWith("/seek")) {
            context.next();
        } else {
            String authorization = request.headers().get(HttpHeaders.AUTHORIZATION);
            if (authorization == null) {
                context.fail(HttpResponseStatus.UNAUTHORIZED.code());

            } else {

                String[] parts = authorization.split(" ");

                if (parts.length < 2) {
                    context.fail(HttpResponseStatus.UNAUTHORIZED.code());
                    return;
                }

                String scheme = parts[0];
                if (scheme.equalsIgnoreCase(OAuthConstant.BEARER)) {
                    JsonObject creds = new JsonObject();
                    creds.put("token", parts[1]);

                    authProvider.authenticate(creds, res -> {
                        if (res.succeeded()) {
                            User authenticated = res.result();
                            authenticated.setAuthProvider(authProvider);
                            context.setUser(authenticated);
                            authorise(authenticated, context);
                        } else {
                            logger.error("Authentication failed", res.cause());
                            context.fail(HttpResponseStatus.UNAUTHORIZED.code());
                        }
                    });

                } else {
                    context.fail(HttpResponseStatus.UNAUTHORIZED.code());
                }
            }
        }
    }
}
