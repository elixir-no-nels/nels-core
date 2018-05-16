'''
Created on Feb 27, 2017

@author: kidane
'''

import json
import tempfile
from os import path

from utils import file_utils, feed_utils

ROOT_PATH = path.dirname(__file__)
TEMP_DIR = tempfile.gettempdir()

# nels master api
API_URL = ""
API_KEY = ""
API_SECRET = ""

# nels storage service
STORAGE_URL = ""
STORAGE_KEY = ""
STORAGE_SECRET = ""

# sbi service
SBI_URL = ""
SBI_KEY = ""
SBI_SECRET = ""


def api_url(relative_url):
    if API_URL.endswith("/"):
        return API_URL + relative_url
    else:
        return "%s/%s" % (API_URL, relative_url)


def storage_url(relative_url):
    if STORAGE_URL.endswith("/"):
        return STORAGE_URL + relative_url
    else:
        return "%s/%s" % (STORAGE_URL, relative_url)


def sbi_url(relative_url):
    if SBI_URL.endswith("/"):
        return SBI_URL + relative_url
    else:
        return "%s/%s" % (SBI_URL, relative_url)


def print_config():
    print ("api service root: %s" % API_URL)
    print ("storage service root: %s" % STORAGE_URL)
    print ("sbi service root: %s" % SBI_URL)


class ConfigKeys():
    API_URL = "API_URL"
    API_KEY = "API_KEY"
    API_SECRET = "API_SECRET"
    STORAGE_URL = "STORAGE_URL"
    STORAGE_KEY = "STORAGE_KEY"
    STORAGE_SECRET = "STORAGE_SECRET"
    SBI_URL = "SBI_URL"
    SBI_KEY = "SBI_KEY"
    SBI_SECRET = "SBI_SECRET"
    NOTIFY_TO = "NOTIFY_TO"


def init():
    global API_URL, API_KEY, API_SECRET, STORAGE_URL, STORAGE_KEY, STORAGE_SECRET, SBI_URL, SBI_KEY, SBI_SECRET
    config_json = json.loads(file_utils.read_file_content(path.join(ROOT_PATH, "config.json")))

    API_URL = config_json[ConfigKeys.API_URL]
    API_KEY = config_json[ConfigKeys.API_KEY]
    API_SECRET = config_json[ConfigKeys.API_SECRET]
    STORAGE_URL = config_json[ConfigKeys.STORAGE_URL]
    STORAGE_KEY = config_json[ConfigKeys.STORAGE_KEY]
    STORAGE_SECRET = config_json[ConfigKeys.STORAGE_SECRET]
    SBI_URL = config_json[ConfigKeys.SBI_URL]
    SBI_KEY = config_json[ConfigKeys.SBI_KEY]
    SBI_SECRET = config_json[ConfigKeys.SBI_SECRET]

    feed_utils.NOTIFY_TO = [config_json[ConfigKeys.NOTIFY_TO]] if ',' not in config_json[ConfigKeys.NOTIFY_TO] else \
        config_json[ConfigKeys.NOTIFY_TO].split(',')


init()

if __name__ == "__main__":
    print_config()
