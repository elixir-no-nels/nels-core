'''
Created on Feb 28, 2017

@author: kidane
'''

import sys
import requests
from utils import feed_utils
import config

def get_ssh_credential(nels_id):
    try:
        response =  requests.get(config.storage_url("users/%s" %nels_id), auth=(config.STORAGE_KEY, config.STORAGE_SECRET))
        if(response.status_code == requests.codes.ok):
            json_response = response.json()
            if u'hostname' in json_response:
                return [json_response[u'hostname'],json_response[u'username'],json_response[u'key-rsa']]
            else:
        
                return [json_response[u'ssh_host'],json_response[u'user_name'],json_response[u'ssh_key']]
    except:
        feed_utils.error(sys.exc_info()[0])
    return None  
