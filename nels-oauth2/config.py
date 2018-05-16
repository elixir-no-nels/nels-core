'''
Created on Apr 25, 2017

@author: kidane
'''
from os import path
import json 

from utils import feed_utils, run_utils, file_utils


PORT = 7600
ENCRYPTION_KEY = 'nels'
PORTAL_URL = ''
ROOT_DIR = path.dirname(__file__)
CLIENT_CREDENTIAL_CLIENTS =[]
IMPLICIT_CLIENTS = []

class ConfigKeys():
    port = "port"
    encrytion_key = "encrytion_key"
    portal_url = "portal_url"
    oauth2_client_credentail_clients = "oauth2_client_credentail_clients"
    oauth2_implicit_clients = "oauth2_implicit_clients"

def configure():
    global PORT, ENCRYPTION_KEY, PORTAL_URL,CLIENT_CREDENTIAL_CLIENTS, IMPLICIT_CLIENTS
    config_pth = path.join(ROOT_DIR, "config.json")
    if not path.exists(config_pth):
        run_utils.exit_fail("missing configuration file")

    config_json = json.loads(file_utils.read_file_content(config_pth))
    feed_utils.info(config_json)
    PORT = int(config_json[ConfigKeys.port])
    ENCRYPTION_KEY = config_json[ConfigKeys.encrytion_key]
    PORTAL_URL = config_json[ConfigKeys.portal_url]
    CLIENT_CREDENTIAL_CLIENTS = config_json[ConfigKeys.oauth2_client_credentail_clients]
    IMPLICIT_CLIENTS  = config_json[ConfigKeys.oauth2_implicit_clients]

def print_config():
    feed_utils.heading("Configurations")
    feed_utils.info("port: %s" % PORT)
    feed_utils.info("implicit clients")
    feed_utils.push_in()
    for client in IMPLICIT_CLIENTS:
        feed_utils.info(client)
    feed_utils.push_out()

if __name__ == "__main__":
    feed_utils.VERBOSE = True
    configure()
    print_config()
