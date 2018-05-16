from optparse import OptionParser

import config 
from api import users 
from utils import feed_utils,args_utils, mail_utils, run_utils


def test_user_del(nels_id):
    feed_utils.heading("Trying user deletion. nels_id : %s" %nels_id)
    if not users.delete_user(nels_id):
        feed_utils.failed("deletion of user failed")
    else:
        feed_utils.ok("deletion of user")
    

if __name__ == "__main__":
    parser = OptionParser(usage='usage: %prog [-v] nels_id')
    parser.add_option('-v', '--verbose', dest="verbose", action="store_true", help='turn verbosity on', default=False)
    (options, args) = parser.parse_args()
    feed_utils.VERBOSE = options.verbose
    
    args_utils.require_args_length(parser, args, 1)
    args_utils.require_arg_number(parser, args, 0, 'nels_id')
    nels_id = int(args[0])
    test_user_del(nels_id)
    