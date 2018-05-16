import json
from os import path

from facades import storage_facade
from utils import file_utils

ROOT_PATH = path.dirname(__file__)

PROJECTS_ROOT_DIR = ''
USERS_ROOT_DIR = ''


class ConfigKeys():
    projects_root_dir = "projects_root_dir"
    users_root_dir = "users_root_dir"


def init():
    global PROJECTS_ROOT_DIR, USERS_ROOT_DIR
    config_json = json.loads(file_utils.read_file_content(path.join(ROOT_PATH, "config.json")))

    # initialize storage settings
    PROJECTS_ROOT_DIR = config_json[ConfigKeys.projects_root_dir]
    storage_facade.PROJECTS_ROOT_DIR = PROJECTS_ROOT_DIR
    USERS_ROOT_DIR = config_json[ConfigKeys.users_root_dir]
    storage_facade.USERS_ROOT_DIR = USERS_ROOT_DIR
