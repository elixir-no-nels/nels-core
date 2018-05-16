from optparse import OptionParser

import config
from facades import storage_facade
from utils import feed_utils, args_utils

if __name__ == "__main__":
    usage = 'usage: %prog [options] name '
    version = '%prog 1.0'

    parser = OptionParser(usage=usage, version=version)
    parser.add_option('-v', '--verbose', dest="verbose", action="store_true", help='turn verbosity on', default=False)
    # get options and arguments
    (options, args) = parser.parse_args()
    feed_utils.VERBOSE = options.verbose
    args_utils.require_args_length(parser, args, 1)

    name = args[0]

    config.init()
    feed_utils.heading("name to nels_id (for users and projects)")
    try:
        feed_utils.ok("name: %s  : nels_id: %s " % (name, storage_facade.sys_to_nels_id(name[1:])))
    except:
        feed_utils.error("unable to convert to nels_id")
