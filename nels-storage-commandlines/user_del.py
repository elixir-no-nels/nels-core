from optparse import OptionParser

import config
from facades import user_facade
from utils import feed_utils, args_utils,run_utils

if __name__ == "__main__":
    usage = 'usage: %prog [options] nels_id'
    version = '%prog 1.0'

    parser = OptionParser(usage=usage, version=version)
    parser.add_option('-v', '--verbose', dest="verbose", action="store_true", help='turn verbosity on', default=False)
    # get options and arguments
    (options, args) = parser.parse_args()
    feed_utils.VERBOSE = options.verbose
    args_utils.require_args_length(parser, args, 1)

    args_utils.require_arg_number(parser, args, 0, "nels_id")
    nels_id = args[0]

    config.init()
    if not user_facade.user_exists(nels_id):
        run_utils.exit_fail("user not found")
    user_facade.user_del(nels_id)