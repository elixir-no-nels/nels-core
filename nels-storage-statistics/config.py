import json
from os import path

import nels_master_api
from utils import file_utils
from facades import storage_facade

ROOT_PATH = path.dirname(__file__)
DRY_RUN = False


class ConfigKeys():
    master_api_url = "master_api_url"
    master_api_client_key = "master_api_client_key"
    master_api_client_secret = "master_api_client_secret"

    projects_root_dir = "projects_root_dir"
    users_root_dir = "users_root_dir"


def init():
    config_json = json.loads(file_utils.read_file_content(path.join(ROOT_PATH, "config.json")))

    # initialize master api settings
    nels_master_api.API_URL = config_json[ConfigKeys.master_api_url]
    nels_master_api.CLIENT_KEY = config_json[ConfigKeys.master_api_client_key]
    nels_master_api.CLIENT_SECRET = config_json[ConfigKeys.master_api_client_secret]

    #initialize storage settings
    storage_facade.PROJECTS_ROOT_DIR = config_json[ConfigKeys.projects_root_dir]
    storage_facade.USERS_ROOT_DIR = config_json[ConfigKeys.users_root_dir]
