# -- coding: utf-8 --

import requests
import sys

import config
from utils import feed_utils


def get_user_ids():
    try:
        ids = []
        response = requests.get(config.sbi_url("users"), auth=(config.SBI_KEY, config.SBI_SECRET))
        if response.status_code == requests.codes.ok:
            json_response = response.json()

            for uid in json_response[u'data']:
                ids.append(uid[u'user_id'])
            return ids
    except:
        feed_utils.error(sys.exc_info()[0])
    return None


def search_users(nels_id=None, idp_number=None, federated_id=None, name=None, email=None, user_type_id=None,
                 is_active=None, affiliation=None):
    print email

    if True:
        json_body = {}

        json_body[u'federated_id'] = []
        print json_body
        response = requests.post(config.sbi_url("/users/query"), json=json_body,
                                 auth=(config.SBI_KEY, config.SBI_SECRET))
        print response

        if response.status_code == requests.codes.ok:
            print response.json()
            return response.json()
    else:
        feed_utils.error(sys.exc_info()[0])
    return None
