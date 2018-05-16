# -*- coding: utf-8 -*-

import sys
import requests

import config
from utils import feed_utils 

#idp constants
FEIDE_IDP = 1
NELS_IDP = 2

#user type constants
ADMINISTRATOR = 1
HELPDESK = 2
NORMAL_USER = 3


def get_nels_ids():
    try:
        ids = []
        response = requests.get(config.api_url("users/ids"), auth=(config.API_KEY, config.API_SECRET))
        if response.status_code == requests.codes.ok:
            json_response = response.json()
            for uid in json_response:
                ids.append(uid[u'id'])
            return ids
    except:
        feed_utils.error(sys.exc_info()[0])
    return None


def get_user(nels_id):
    try:
        response = requests.get(config.api_url("users/%s" % nels_id), auth=(config.API_KEY, config.API_SECRET))
        if response.status_code == requests.codes.ok:
            return response.json()
    except:
        feed_utils.error(sys.exc_info()[0])
    return None


def register_user(idp_number,federated_id,name,email,user_type_id,is_active,affiliation):
    try:
        json_body = {"idpnumber": idp_number, "idpusername": federated_id, "usertype": user_type_id, "isactivie": is_active, "name": name, "email": email, "affiliation": affiliation}
        response = requests.post(config.api_url("users"), json=json_body, auth=(config.API_KEY, config.API_SECRET))
        if response.status_code == requests.codes.created:
            return response.json()["id"]
    except:
        feed_utils.error(sys.exc_info()[0])
    return None


def search_users(nels_id=None, idp_number=None, federated_id=None, name=None, email=None, user_type_id=None, is_active=None, affiliation=None):
    try:
        json_body = {}
        if nels_id:
            json_body['id'] = nels_id
        if idp_number:
            json_body['idpnumber'] = idp_number
        if federated_id:
            json_body['idpusername'] = federated_id
        if name:
            json_body['name'] = name 
        if email:
            json_body['email'] = email
        if user_type_id:
            json_body['usertype'] = user_type_id
        if is_active:
            json_body['isactivie'] = is_active
        if affiliation:
            json_body['affiliation'] = affiliation
            
        response = requests.post(config.api_url("users/query"), json=json_body, auth=(config.API_KEY, config.API_SECRET))
        if response.status_code == requests.codes.ok:
            return response.json()
    except:
        feed_utils.error(sys.exc_info()[0])
    return None


def delete_user(nels_id):
    try:
        response = requests.delete(config.api_url("users/%s" % nels_id), auth=(config.API_KEY, config.API_SECRET))
        return response.status_code == requests.codes.no_content
    except :
        feed_utils.error(sys.exc_info()[0])
    return False
