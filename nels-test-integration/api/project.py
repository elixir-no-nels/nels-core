__author__ = 'Xiaxi Li'
__email__ = 'xiaxi.li@ii.uib.no'
__date__ = '14/Mar/2017'

import sys
import requests

import config
from utils import feed_utils


def add_user_to_project(project_id, user_id, membership_type):
    try:
        json_body = {"membership_type": membership_type}
        response = requests.post(config.api_url("projects/%s/users/%s" % (project_id, user_id)), json=json_body, auth=(config.API_KEY, config.API_SECRET))
        return response.status_code == requests.codes.ok
    except:
        feed_utils.error(sys.exc_info()[0])
    return None


def delete_user_from_project(project_id, user_id):
    try:
        response = requests.delete(config.api_url("projects/%s/users/%s" % (project_id, user_id)), auth=(config.API_KEY, config.API_SECRET))
        return response.status_code == requests.codes.no_content
    except:
        feed_utils.error(sys.exc_info()[0])
    return None


def get_user_in_project(project_id, user_id):
    try:
        response = requests.get(config.api_url("projects/%s/users/%s" % (project_id, user_id)),
                                   auth=(config.API_KEY, config.API_SECRET))
        if response.status_code == requests.codes.ok:
            json_response = response.json()
            return int(json_response["membership_type"])
    except:
        feed_utils.error(sys.exc_info()[0])
    return None


def get_project(project_id):
    try:
        response = requests.get(config.api_url("projects/%s" % project_id), auth=(config.API_KEY, config.API_SECRET))
        if response.status_code == requests.codes.ok:
            return response.json()
    except:
        feed_utils.error(sys.exc_info()[0])
    return None


def get_project_ids():
    try:
        ids = []
        response = requests.get(config.api_url("projects/ids"), auth=(config.API_KEY, config.API_SECRET))
        if response.status_code == requests.codes.ok:
            json_response = response.json()
            for uid in json_response:
                ids.append(uid[u'id'])
            return ids
    except:
        feed_utils.error(sys.exc_info()[0])
    return None


def add_project(name, description):
    try:
        json_body = {"name": name, "description": description}
        response = requests.post(config.api_url("projects"), json=json_body, auth=(config.API_KEY, config.API_SECRET))
        if response.status_code == requests.codes.created:
            return response.json()["id"]
    except:
        feed_utils.error(sys.exc_info()[0])
    return None


def delete_project(project_id):
    try:
        response = requests.delete(config.api_url("projects/%s" % project_id), auth=(config.API_KEY, config.API_SECRET))
        return response.status_code == requests.codes.no_content
    except:
        feed_utils.error(sys.exc_info()[0])
    return False


def search_projects(project_id=None, name=None):
    try:
        json_body = {}
        if project_id:
            json_body['id'] = project_id
        if name:
            json_body['name'] = name

        response = requests.post(config.api_url("projects/query"), json=json_body,
                                 auth=(config.API_KEY, config.API_SECRET))
        if response.status_code == requests.codes.ok:
            return response.json()
    except:
        feed_utils.error(sys.exc_info()[0])
    return None