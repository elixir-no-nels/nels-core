
from os import path
from optparse import OptionParser

from utils import feed_utils, args_utils,run_utils,ssh_utils
from api import storage
import config

def test_ssh(nels_id):
    feed_utils.heading("ssh use case. nels_id: %s" % nels_id)
    credentail =  ssh_utils.get_ssh_credentials(nels_id)
    if not credentail:
        feed_utils.failed("fetching key")
        return False
    feed_utils.ok("ssh key fetched")
    (host, username, key_file) = credentail
    (status,items,error) = run_utils.launch_remote_with_key(key_file, username, host, "ls ") 
    if status != 0:
        feed_utils.error(error)
        return False

    if "Personal" in items:
        feed_utils.ok("Personal folder found")
    else:
        feed_utils.failed("Personal folder not found")
    
    if "Projects" in items:
        feed_utils.ok("Projects folder found")
    else:
        feed_utils.failed("Projects folder not found")
    
    feed_utils.info("cleaning key file")
    run_utils.launch_cmd("rm -f  %s" % key_file)
    return True

if __name__ == "__main__":
    parser = OptionParser(usage='usage: %prog nels_id')
    parser.add_option('-v', '--verbose', dest="verbose", action="store_true", help='turn verbosity on', default=False)
    (options, args) = parser.parse_args()
    feed_utils.VERBOSE = options.verbose
    args_utils.require_args_length(parser, args, 1)
    args_utils.require_arg_number(parser, args, 0, 'nels_id')
    test_ssh(int(args[0]))
