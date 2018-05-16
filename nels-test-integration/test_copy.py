'''
Created on Feb 28, 2017

@author: kidane
'''
from os import path
from optparse import OptionParser

from utils import feed_utils, args_utils,ssh_utils, run_utils
from api import storage, job
import config

def test_copy(nels_id):
    feed_utils.heading("copy use case. nels_id: %s" % nels_id)
    
    credentail =  ssh_utils.get_ssh_credentials(nels_id)
    if not credentail:
        feed_utils.failed("fetching key")
        return False
    feed_utils.ok("ssh key fetched")
    (host, username, key_file) = credentail
    
    src_file = "Personal/dummy.txt"
    dest_dir = "Personal/dummy"
    dest_file = "Personal/dummy/dummy.txt"
    
    if not ssh_utils.write_dummy_file(key_file, username, host, src_file, "5M"):
        feed_utils.failed("write test file")
        return False
    feed_utils.ok("write test file")
    
    if not ssh_utils.create_folder(key_file, username, host, dest_dir):
        feed_utils.failed("create test directory")
        return False
    feed_utils.ok("create test directory")
    
    job_id = job.add_job(job.STORAGE_COPY, nels_id, [src_file], dest_dir)
    if job_id == -1:
        feed_utils.failed("job submission")
        return False
    feed_utils.ok("job submission. job-id: %s" % job_id)
    job.wait_for_job(job_id)
    #validate file
    [exit_code,output,error] = run_utils.launch_remote_with_key(key_file, username, host, "diff %s %s" % (src_file,dest_file))
    if not exit_code == 0:
        feed_utils.error(error)
        return False
    if output != "":
        feed_utils.failed("validating copied file")
        return False
    feed_utils.ok("validating copied file")
    
    #clean up
    if not job.delete_job(job_id):
        feed_utils.failed("job delete")
        return  False
    feed_utils.ok("job delete")
    [exit_code,output, error] = run_utils.launch_remote_with_key(key_file, username, host, "rm -rf %s %s" % (dest_dir, src_file))
    if exit_code != 0:
        feed_utils.error(error)
        return False
    feed_utils.ok("clean up")
    return True

if __name__ == "__main__":
    parser = OptionParser(usage='usage: %prog nels_id')
    parser.add_option('-v', '--verbose', dest="verbose", action="store_true", help='turn verbosity on', default=False)
    (options, args) = parser.parse_args()
    feed_utils.VERBOSE = options.verbose
    args_utils.require_args_length(parser, args, 1)
    args_utils.require_arg_number(parser, args, 0, 'nels_id')
    test_copy(int(args[0]))