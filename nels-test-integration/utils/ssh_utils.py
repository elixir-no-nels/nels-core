'''
Created on Feb 28, 2017

@author: kidane
'''

from os import path

import config
from api import storage
from utils import feed_utils, file_utils, run_utils

def get_ssh_credentials(nels_id):
    credentail =  storage.get_ssh_credential(nels_id)
    if not credentail:
        return None
    (host,username) = (credentail[0],credentail[1])
    feed_utils.info("fetching keys. host: %s, username:%s " % (host,username))
    key_file = path.join(config.TEMP_DIR,"%s.nels" %nels_id)
    feed_utils.info("writing key file: %s" %key_file)
    file_utils.write_to_file(key_file, credentail[2])
    run_utils.launch_cmd("chmod 600 %s" % key_file)
    return [credentail[0],credentail[1],key_file]

def write_dummy_file(key_file,username,host, file_path,size):
    cmd = "dd if=/dev/zero of=%s  bs=%s count=1" % (file_path, size)
    [exit_code,output, error] = run_utils.launch_remote_with_key(key_file, username, host, cmd)
    return exit_code == 0

def create_folder(key_file,username,host, folder_path):
    cmd = "mkdir %s " % (folder_path)
    [exit_code,output,error] = run_utils.launch_remote_with_key(key_file, username, host, cmd)
    return exit_code == 0
    