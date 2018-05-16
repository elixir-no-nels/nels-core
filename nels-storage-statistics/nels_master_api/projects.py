
import requests
import nels_master_api

def get_project_ids():
    try:
        ids = []
        response = requests.get(nels_master_api.get_full_url("projects/ids" ),auth=(nels_master_api.CLIENT_KEY, nels_master_api.CLIENT_SECRET))
        if(response.status_code == requests.codes.ok):
            json_response = response.json()
            for uid in json_response:
                ids.append(uid[u'id'])
            return ids
    except:
        return None