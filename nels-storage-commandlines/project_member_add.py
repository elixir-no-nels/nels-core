from optparse import OptionParser

import config
from facades import project_facade
from utils import feed_utils, args_utils

if __name__ == "__main__":
    usage = 'usage: %prog [options] project_id nels_id  role {member, poweruser, admin}'
    version = '%prog 1.0'

    parser = OptionParser(usage=usage, version=version)
    parser.add_option('-v', '--verbose', dest="verbose", action="store_true", help='turn verbosity on', default=False)
    # get options and arguments
    (options, args) = parser.parse_args()
    feed_utils.VERBOSE = options.verbose
    args_utils.require_args_length(parser, args, 3)

    args_utils.require_arg_number(parser, args, 0, "project_id")
    args_utils.require_arg_number(parser, args, 1, "nels_id")

    config.init()
    project_facade.project_user_add(args[0], args[1], args[2])
