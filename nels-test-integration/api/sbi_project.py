# -*- coding: utf-8 -*-

import sys
import requests

import config
from utils import feed_utils


def add_user_to_project(project_id, membership_type, federated_id):
    try:
        json_data_array = [{"federated_id": federated_id, "role": membership_type}]
        json_body = {"method": "add", "data": json_data_array}

        response = requests.post(config.sbi_url("projects/%s/users/do" % (project_id)), json=json_body,
                                 auth=(config.SBI_KEY, config.SBI_SECRET))
        return response.status_code == requests.codes.ok
    except:
        feed_utils.error(sys.exc_info()[0])
    return None


def delete_user_from_project(project_id, federated_id):
    try:
        json_data_array = [federated_id]
        json_body = {"method": "delete", "data": json_data_array}

        response = requests.post(config.sbi_url("projects/%s/users/do" % (project_id)), json=json_body,
                                 auth=(config.SBI_KEY, config.SBI_SECRET))
        return response.status_code == requests.codes.ok


    except:
        feed_utils.error(sys.exc_info()[0])
    return None


def get_users_in_project(project_id):
    try:

        response = requests.get(config.sbi_url("projects/%s/users" % (project_id)),
                                auth=(config.SBI_KEY, config.SBI_SECRET))

        if response.status_code == requests.codes.ok:
            json_response = response.json()
            return json_response
        else:
            feed_utils.info(response.text)

    except:
        feed_utils.error(sys.exc_info()[0])
    return None


def get_project(project_id):
    try:
        response = requests.get(config.sbi_url("projects/%s" % project_id), auth=(config.SBI_KEY, config.SBI_SECRET))

        if response.status_code == requests.codes.ok:
            return response.json()
    except:
        feed_utils.error(sys.exc_info()[0])
    return None


def get_datasets_in_project(project_id):
    try:
        ids = []

        response = requests.get(config.sbi_url("projects/%s/datasets" % project_id),
                                auth=(config.SBI_KEY, config.SBI_SECRET))

        if response.status_code == requests.codes.ok:

            json_response = response.json()

            if json_response == []:
                """project without datasets"""
                return json_response
            else:
                for uid in json_response:
                    if u'id' in uid:
                        ids.append(uid[u'id'])
                return ids

    except:
        feed_utils.error(sys.exc_info()[0])
    return None


def get_dataset_in_project(project_id, dataset_id):
    try:
        response = requests.get(config.sbi_url("projects/%s/datasets/%s" % (project_id, dataset_id)),
                                auth=(config.SBI_KEY, config.SBI_SECRET))

        if response.status_code == requests.codes.ok:
            return response.json()
    except:
        feed_utils.error(sys.exc_info()[0])
    return None


def add_dataset_to_project(project_id, federated_id):
    try:
        json_dataset_type_array = get_dataset_types()
        json_body = {"data_set_type_id": json_dataset_type_array["data"][0]["id"],
                     "name": "dataset integration",
                     "description": "test"}

        headers = {'federated-id': federated_id}

        response = requests.post(config.sbi_url("projects/%s/datasets" % (project_id)), headers=headers,
                                 json=json_body, auth=(config.SBI_KEY, config.SBI_SECRET))

        if response.status_code == requests.codes.created:

            return True
        else:
            feed_utils.info(response.status_code)
    except:
        feed_utils.error(sys.exc_info()[0])
    return None


def delete_dataset_in_project(project_id, dataset_id):
    try:
        response = requests.delete(config.sbi_url("projects/%s/datasets/%s" % (project_id, dataset_id)),
                                   auth=(config.SBI_KEY, config.SBI_SECRET))
        if response.status_code == requests.codes.no_content:
            return True
        else:
            feed_utils.info(response.status_code)
            feed_utils.info(response.text)
    except:
        feed_utils.error(sys.exc_info()[0])
    return None


def get_dataset_types():
    try:
        response = requests.get(config.sbi_url("datasettypes"),
                                auth=(config.SBI_KEY, config.SBI_SECRET))
        if response.status_code == requests.codes.ok:
            return response.json()
    except:
        feed_utils.error(sys.exc_info()[0])
    return None


def get_subtype_in_dataset_in_project(project_id, dataset_id, subtype_id):
    try:
        response = requests.get(
            config.sbi_url("projects/%s/datasets/%s/subtypes/%s" % (project_id, dataset_id, subtype_id)),
            auth=(config.SBI_KEY, config.SBI_SECRET))

        if response.status_code == requests.codes.ok:
            return response.json()
    except:
        feed_utils.error(sys.exc_info()[0])
    return None


def get_subtypes_in_dataset_in_project(project_id, dataset_id):
    try:
        ids = []

        response = requests.get(config.sbi_url("projects/%s/datasets/%s/subtypes" % (project_id, dataset_id)),
                                auth=(config.SBI_KEY, config.SBI_SECRET))

        if response.status_code == requests.codes.ok:

            json_response = response.json()

            if json_response == []:
                """project dataset without subtypes"""
                return json_response
            else:
                for uid in json_response:
                    if u'id' in uid:
                        feed_utils.info(uid)
                        ids.append(uid[u'id'])
                return ids
        else:
            feed_utils.debug(response.status_code)
            feed_utils.info(response.text)
    except:
        feed_utils.error(sys.exc_info()[0])
    return None


def get_project_ids():
    try:
        ids = []
        response = requests.get(config.sbi_url("projects/"), auth=(config.SBI_KEY, config.SBI_SECRET))

        if response.status_code == requests.codes.ok:
            json_response = response.json()
            for uid in json_response[u'data']:
                ids.append(uid[u'project_id'])
            return ids
    except:
        feed_utils.error(sys.exc_info()[0])
    return None


def recompute_project_disc_usage():
    try:
        json_body = {"method": "re_populate_project_disk_usage"}
        response = requests.post(config.sbi_url("projects/do"), json=json_body,
                                 auth=(config.SBI_KEY, config.SBI_SECRET))

        if response.status_code == requests.codes.ok:
            return response.status_code == requests.codes.ok
        else:
            feed_utils.info(response.status_code)
    except:
        feed_utils.error(sys.exc_info()[0])
    return False


def add_project(quota_id, name, federated_id):
    try:

        json_body = {u'name': u'%s' % name,
                     u'description': u'added from integration test',
                     u'quota_id': int(quota_id),
                     u'federated_id': u'%s' % federated_id,
                     u'contact_person': u'Contact Person',
                     u'contact_email': u'%s' % federated_id,
                     u'contact_affiliation': u'UiB'}

        response = requests.post(config.sbi_url("projects/"), json=json_body, auth=(config.SBI_KEY, config.SBI_SECRET))

        if response.status_code == requests.codes.created:
            return response.json()["id"]
    except:

        feed_utils.error(sys.exc_info()[0])
    return None


def update_project(json_body_update_project, project_id):
    try:
        json_body = json_body_update_project
        response = requests.put(config.sbi_url("projects/%s" % project_id), json=json_body,
                                auth=(config.SBI_KEY, config.SBI_SECRET))

        if response.status_code == requests.codes.no_content:
            return project_id
    except:

        feed_utils.error(sys.exc_info()[0])
    return None


def delete_project(federated_id, project_id):
    try:

        json_body = {u'federated_id': u'%s' % federated_id}
        response = requests.post(config.sbi_url("projects/%s" % project_id), json=json_body,
                                 auth=(config.SBI_KEY, config.SBI_SECRET))

        if response.status_code == requests.codes.no_content:
            return True
        else:
            feed_utils.debug(response.status_code)
            feed_utils.info(response.text)

    except:
        feed_utils.error(sys.exc_info()[0])
    return None


def search_projects(project_id=None, name=None):
    try:
        json_body = {}
        if project_id:
            json_body['id'] = project_id
            json_body['query'] = project_id
        if name:
            json_body['name'] = name
            json_body['query'] = name

        response = requests.post(config.sbi_url("projects/query"), json=json_body,
                                 auth=(config.SBI_KEY, config.SBI_SECRET))

        feed_utils.debug("Response headers: %s" % response.headers)

        if response.status_code == requests.codes.ok:
            return response.json()
    except:
        feed_utils.error(sys.exc_info()[0])
    return None
