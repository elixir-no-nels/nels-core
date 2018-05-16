'''
Created on Mar 3, 2017

@author: kidane
'''

import json

import oauth2.grant
import oauth2.store.memory
import oauth2.tokengenerator
import oauth2.tokengenerator
import oauth2.web
import tornado.ioloop
import tornado.web
from oauth2.error import UserNotAuthenticated
from tornado import gen

import config
from utils import tornado_utils, feed_utils, oauth2_utils, security_utils, string_utils


class OAuth2Handler(tornado.web.RequestHandler):
    def initialize(self, controller):
        self.controller = controller

    @gen.coroutine
    def post(self):
        oauth_request = self.request
        # dispatch request
        oauth_request.post_param = lambda key: json.loads(oauth_request.body.decode())[key]
        oauth_response = self.controller.dispatch(oauth_request, environ={})
        # map response
        for name, value in list(oauth_response.headers.items()):
            self.set_header(name, value)

        self.set_status(oauth_response.status_code)
        if oauth_response.status_code == 200:
            response_json = json.loads(oauth_response.body)
            token_info = self.controller.access_token_store.access_tokens.get(response_json["access_token"])
            if token_info.expires_at:
                response_json[".expires"] = token_info.expires_at
            self.write(response_json)
            tornado_utils.coroutine_return()
        self.write(oauth_response.body)

    @gen.coroutine
    def get(self):
        try:
            response_type = self.get_argument("response_type", None)
            if not response_type:
                tornado_utils.return_with_status(self, 401, "error", "missing parameters")
            if response_type not in ["token", "code"]:
                tornado_utils.return_with_status(self, 401, "error", "missing parameters")
            params = {}
            for k, v in self.request.arguments.items():
                params[k] = v[0] if len(v) == 1 else None

            if 'state' not in params.keys():
                params['state'] = None

            feed_utils.info(params)
            # dispatch request
            oauth_request = self.request
            oauth_request.get_param = lambda key: params[key]
            oauth_response = self.controller.dispatch(oauth_request, environ={})
            # map response
            for name, value in list(oauth_response.headers.items()):
                self.set_header(name, value)

            feed_utils.info("%s : %s" % (oauth_response.status_code,oauth_response.body))

            self.set_status(oauth_response.status_code)
            self.write(oauth_response.body)
        except Exception as err:
            self.set_header('Content-Type', 'application/json')
            self.set_status(401)
            self.finish(json.dumps({'error': str(err)}))


class SecureRequestHandler(tornado.web.RequestHandler):
    def initialize(self, controller):
        self.controller = controller
        self.token_info = None

    # authenticate tokens
    def prepare(self):
        try:
            auth_header = self.request.headers.get('Authorization', None)
            if not auth_header:
                raise Exception('This resource need a authorization token')

            token = auth_header[7:]
            self.token_info = self.controller.access_token_store.access_tokens.get(token)
            if not self.token_info:
                raise Exception('Invalid Token')

            if self.token_info.is_expired():
                raise Exception('Expired token')

        except Exception as err:
            self.set_header('Content-Type', 'application/json')
            self.set_status(401)
            self.finish(json.dumps({'error': str(err)}))


class OAuth2IntrospectHandler(SecureRequestHandler):
    @gen.coroutine
    def post(self):
        if "resource_owner" not in self.token_info.scopes:
            tornado_utils.return_with_status(self, 401, "error", "Insufficient Privileges")

        token = self.get_argument('token', None)
        if not token:
            tornado_utils.return_with_status(self, 400, "error", "missing token")

        introspect_token = self.controller.access_token_store.access_tokens.get(token)
        if not introspect_token:
            self.write({"active": False})
            tornado_utils.coroutine_return()

        if introspect_token.is_expired():
            self.write({"active": False})
            tornado_utils.coroutine_return()

        reply_info = {"active": True,
                      "scope": oauth2_utils.scope_array_to_list(introspect_token.scopes),
                      "client_id": introspect_token.client_id
                      }

        if introspect_token.data:
            try:
                reply_info[".nels_id"] = introspect_token.data["nels_id"]
                reply_info[".name"] = introspect_token.data["name"]
                reply_info[".user_type"] = introspect_token.data["user_type"]
                reply_info[".federated_id"] = introspect_token.data["federated_id"]
            except:
                pass

        if introspect_token.expires_at:
            reply_info["exp"] = introspect_token.expires_at
        self.set_header('Content-Type', 'application/json')
        self.write(json.dumps(reply_info))


class NelsSiteAdapter(oauth2.web.AuthorizationCodeGrantSiteAdapter,
                      oauth2.web.ImplicitGrantSiteAdapter):
    def render_auth_page(self, request, response, environ, scopes, client):
        response.status_code = 302
        response_type = "token"
        if "response_type" in request.query_arguments.keys():
            response_type = request.get_param("response_type")
        login_url = "%s?oac=%s&response_type=%s" % (config.PORTAL_URL, client.identifier, response_type)
        if "state" in request.query_arguments.keys():
            login_url = string_utils.append_with_delimiter(login_url, "state=%s" % request.get_param("state"), "&")
        response.add_header("Location", login_url)
        return response

    def authenticate(self, request, environ, scopes, client):
        if request.method == "GET":
            if "nels_token" in request.query_arguments.keys():
                feed_utils.info("nels_token %s" % request.get_param("nels_token"))
                try:
                    expanded_token = json.loads(
                        security_utils.decrypt(config.ENCRYPTION_KEY, request.get_param("nels_token")))
                    feed_utils.info(expanded_token)
                    return (expanded_token, expanded_token["nels_id"])
                except Exception as ex:
                    feed_utils.error(ex.message)
                    pass
        raise UserNotAuthenticated

    def user_has_denied_access(self, request):
        if request.method == "GET":
            if "nels_deny" in request.query_arguments.keys():
                return True
        return False


def main():
    feed_utils.VERBOSE = True
    config.configure()
    # memory client store
    client_store = oauth2.store.memory.ClientStore()

    for client in config.IMPLICIT_CLIENTS:
        client_store.add_client(client_id=str(client["client_id"]),
                                client_secret=str(client["client_secret"]),
                                redirect_uris=str([client["redirect_uri"]]),
                                authorized_grants=[oauth2.grant.ImplicitGrant.grant_type,
                                                   oauth2.grant.AuthorizationCodeGrant.grant_type]
                                )

    for client in config.CLIENT_CREDENTIAL_CLIENTS:
        client_store.add_client(client_id=client["client_id"],
                                client_secret=client["client_secret"],
                                redirect_uris=[""],
                                authorized_grants=[oauth2.grant.ClientCredentialsGrant.grant_type],
                                )

    # memory token store
    token_store = oauth2.store.memory.TokenStore()

    # Generator of tokens
    token_generator = oauth2.tokengenerator.Uuid4()
    token_generator.expires_in[oauth2.grant.ClientCredentialsGrant.grant_type] = 3600
    token_generator.expires_in[oauth2.grant.ImplicitGrant.grant_type] = 3600
    token_generator.expires_in[oauth2.grant.AuthorizationCodeGrant.grant_type] = 3600

    # OAuth2 controller
    auth_controller = oauth2.Provider(
        access_token_store=token_store,
        auth_code_store=token_store,
        client_store=client_store,
        token_generator=token_generator
    )
    # auth_controller.token_path = '/token'
    # auth_controller.authorize_path = '/authorize'

    site_adapter = NelsSiteAdapter()
    # Add Client Credentials to OAuth2 controller
    auth_controller.add_grant(oauth2.grant.ClientCredentialsGrant(scopes=["resource_owner","component"]))
    auth_controller.add_grant(
        oauth2.grant.AuthorizationCodeGrant(scopes=["user", "admin", "helpdesk"], site_adapter=site_adapter,
                                            expires_in=3600))
    auth_controller.add_grant(
        oauth2.grant.ImplicitGrant(scopes=["user", "admin", "helpdesk"], site_adapter=site_adapter))
    auth_controller.add_grant(oauth2.grant.RefreshToken(expires_in=2592000))

    handlers = [
        (r'/token', OAuth2Handler, dict(controller=auth_controller)),
        (r'/authorize', OAuth2Handler, dict(controller=auth_controller)),
        (r'/introspect', OAuth2IntrospectHandler, dict(controller=auth_controller)),
    ]
    settings = {
        "debug": True,
    }

    # Create Tornado application
    app = tornado.web.Application(handlers,**settings)


    print("Server Starting on port: %s" % config.PORT)
    # Start Server
    app.listen(config.PORT)

    tornado.ioloop.IOLoop.instance().start()


if __name__ == "__main__":
    main()
