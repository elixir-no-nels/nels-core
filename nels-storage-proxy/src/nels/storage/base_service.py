'''
Created on Jan 21, 2016

@author: Kidane
'''

from nels.storage.config import storage_config
import requests

def get_url(relative_url):
        if storage_config.BASE_SERVICE_URL.endswith("/"):
            return storage_config.BASE_SERVICE_URL + relative_url
        else:
            return "%s/%s" %(storage_config.BASE_SERVICE_URL,relative_url)

def get_ssh_credential(nels_id):
    try:
        response = requests.get(get_url("users/%s" % nels_id),auth=(storage_config.USERNAME, storage_config.PASSWORD))
        if(response.status_code == requests.codes.ok):
            return response.json()
    except:
        pass
    return None
