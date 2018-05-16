import requests

import nels_master_api
from utils import feed_utils

PERSONAL_DISK_USAGE = 100
PERSONAL_DISK_USAGE_SUMMARY = 101
PROJECT_DISK_USAGE = 200
PROJECT_DISK_USAGE_SUMMARY = 201


def get_stats(context_id):
    try:
        stats = []
        response = requests.get(nels_master_api.get_full_url("stats/contexts/%s" % context_id),
                                auth=(nels_master_api.CLIENT_KEY, nels_master_api.CLIENT_SECRET))
        if (response.status_code == requests.codes.ok):
            json_response = response.json()
            for lg in json_response:
                stats.append([lg[u'id'], lg[u'statscontextid'], lg[u'targetid'], lg[u'value'], lg[u'statstime']])
            return stats
    except Exception as ex:
        feed_utils.error(ex)
        return None


def add_stat(context_id, target_id, value):
    try:
        response = requests.post(nels_master_api.get_full_url("stats/add"),
                                 json={'contextId': context_id, 'targetId': target_id, 'value': value},
                                 auth=(nels_master_api.CLIENT_KEY, nels_master_api.CLIENT_SECRET))
        return response.status_code == requests.codes.created
    except:
        pass
    return False
