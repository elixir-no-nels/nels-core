from optparse import OptionParser

import config
from facades import user_facade
from utils import feed_utils, args_utils

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
    user_facade.user_add(nels_id)

    feed_utils.ok("nels-id:\t%s\nusername:\t%s\nhome folder:%s" % (
        nels_id, user_facade.nels_id_to_username(nels_id), user_facade.user_home(nels_id)))
