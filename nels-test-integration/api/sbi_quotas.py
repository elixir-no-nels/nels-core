__author__ = 'Siri Kallhovd'
__email__ = 'siri.kallhovd@uib.no'
__date__ = '24/Jan/2018'

import sys
import requests

import config
from utils import feed_utils


def get_quota_ids():
    try:
        ids = []
        response = requests.get(config.sbi_url("quotas/"), auth=(config.SBI_KEY, config.SBI_SECRET))
        feed_utils.info(response.headers)
        feed_utils.info(response.json())

        if response.status_code == requests.codes.ok:
            json_response = response.json()
            for uid in json_response[u'data']:
                ids.append(uid[u'id'])
            feed_utils.info(ids)
            return ids
    except:
        feed_utils.error(sys.exc_info()[0])
    return None


def get_quota(quota_id):
    try:
        response = requests.get(config.sbi_url("quotas/%s" % quota_id), auth=(config.SBI_KEY, config.SBI_SECRET))

        if response.status_code == requests.codes.ok:
            return response.json()
    except:
        feed_utils.error(sys.exc_info()[0])
    return None


def get_projects_in_quota(quota_id):
    try:
        ids = []
        response = requests.get(config.sbi_url("quotas/%s/projects" % (quota_id)),
                                auth=(config.SBI_KEY, config.SBI_SECRET))
        feed_utils.info(response.json())
        if response.status_code == requests.codes.ok:
            json_response = response.json()
            if json_response[u'count'] > 0:

                for uid in json_response[u'data']:
                    ids.append(uid[u'project_id'])

            else:
                feed_utils.ok("No projects in this quota")
            return ids
    except:
        feed_utils.error(sys.exc_info()[0])
    return None


def add_quota(name, federated_id):
    try:

        json_body = {u'name': u'%s' % name,
                     u'description': u'added from integration test',
                     u'federated_id': u'%s' % federated_id,
                     u'quota_size': 1000000000000}

        response = requests.post(config.sbi_url("quotas/"), json=json_body, auth=(config.SBI_KEY, config.SBI_SECRET))

        if response.status_code == requests.codes.created:
            return requests.codes.created
        feed_utils.info(response.text)
    except:
        feed_utils.error(sys.exc_info()[0])
    return None


def delete_quota(id):
    try:
        response = requests.delete(config.sbi_url("quotas/%s" % id), auth=(config.SBI_KEY, config.SBI_SECRET))
        return response.status_code == requests.codes.no_content
    except:
        feed_utils.error(sys.exc_info()[0])
    return False


def search_quotas(name):
    try:
        json_body = {'name': name, 'query': name}

        response = requests.post(config.sbi_url("quotas/query"), json=json_body,
                                 auth=(config.SBI_KEY, config.SBI_SECRET))

        if response.status_code == requests.codes.ok:
            return response.json()

        feed_utils.debug(response.status_code)
    except:
        feed_utils.error(sys.exc_info()[0])
    return None
