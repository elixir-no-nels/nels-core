# -*- coding: utf-8 -*-

'''
Created on Sep 2, 2016

@author: Kidane
'''
import requests
import sys

import config
from utils import feed_utils

STORAGE_COPY = 100
STORAGE_MOVE = 101
SBI_PUSH = 106 #norstore: 102
SBI_PULL = 107 #norstore: 103
TSD_PUSH = 104
TSD_PULL = 105


def add_job(job_type_id, nels_id, src_items, dest_folder):
    try:
        url = config.api_url("jobs/add")
        if job_type_id == STORAGE_COPY or job_type_id == STORAGE_MOVE:
            params = {"nels_id": nels_id, "job_type_id": job_type_id,
                      "parameters": {"source": src_items, "destination": dest_folder}}

        response = requests.post(url, json=params, auth=(config.API_KEY, config.API_SECRET))
        if (response.status_code == requests.codes.created):
            return response.json()['job_id']
    except:
        feed_utils.error(sys.exc_info()[0])
    return -1


def add_sbi_job(job_type_id, nels_id, files_list, folders_list, remote_host, user_name, ssh_key, dataset_id, dataset,
                subtype, subtype_id, parent_path_of_source, destination_path):
    try:
        url = config.api_url("jobs/add")
        if job_type_id == SBI_PUSH or job_type_id == SBI_PULL:
            params = {"nels_id": nels_id,
                      "job_type_id": job_type_id,
                      "parameters": {"remote_host": remote_host,
                                     "user_name": user_name,
                                     "ssh_key": ssh_key,
                                     "dataset_id": dataset_id,
                                     "dataset": dataset,
                                     "subtype": subtype,
                                     "subtype_id": subtype_id,
                                     "parent_path_of_source": parent_path_of_source,
                                     "destination_path": destination_path,
                                     "files": files_list,
                                     "folders": folders_list
                                     }}

        feed_utils.info("Job type id: %s" % params["job_type_id"])
        response = requests.post(url, json=params, auth=(config.API_KEY, config.API_SECRET))
        if (response.status_code == requests.codes.created):
            feed_utils.info(response.text)
            return response.json()['job_id']
        else:
            feed_utils.info(response.text)
    except:
        feed_utils.error(sys.exc_info()[0])
    return -1


def get_job_info(job_id):
    try:
        url = config.api_url("jobs/%s" % job_id)
        params = {"jobId": job_id}
        response = requests.get(url, json=params, auth=(config.API_KEY, config.API_SECRET))
        if (response.status_code == requests.codes.ok):
            return response.json();
    except:
        feed_utils.error(sys.exc_info()[0])
    return None


def delete_job(job_id):
    try:
        url = config.api_url("jobs/%s" % job_id)
        response = requests.delete(url, auth=(config.API_KEY, config.API_SECRET))
        return (response.status_code == requests.codes.no_content)
    except:
        feed_utils.error(sys.exc_info()[0])
    return False


def wait_for_job(job_id):
    completion = -1
    job_info = get_job_info(job_id)
    while (job_info["completion"] != 100):
        if (job_info["completion"] != completion):
            completion = job_info["completion"]
            feed_utils.info("waiting for job to finish: completion %s" % completion)
        job_info = get_job_info(job_id)
    feed_utils.info("job completed")
