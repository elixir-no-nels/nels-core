
import requests
import nels_master_api

def get_nels_ids():
    try:
        ids = []
        response = requests.get(nels_master_api.get_full_url("users/ids" ),auth=(nels_master_api.CLIENT_KEY, nels_master_api.CLIENT_SECRET))
        if(response.status_code == requests.codes.ok):
            json_response = response.json()
            for uid in json_response:
                ids.append(uid[u'id'])
            return ids
    except:
        return None

def get_user(nels_id):
    try:
        response = requests.get(nels_master_api.get_full_url("users/%s" %nels_id ),auth=(nels_master_api.CLIENT_KEY, nels_master_api.CLIENT_SECRET))
        if(response.status_code == requests.codes.ok):
            return response.json()
    except:
        return None