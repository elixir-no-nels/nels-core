from optparse import OptionParser
from os import path

import config
from facades import storage_facade, user_facade
from utils import feed_utils, args_utils, run_utils

if __name__ == "__main__":
    usage = 'usage: %prog [options] nels_id  link_to_clean'
    version = '%prog 1.0'

    parser = OptionParser(usage=usage, version=version)
    parser.add_option('-v', '--verbose', dest="verbose", action="store_true", help='turn verbosity on', default=False)
    # get options and arguments
    (options, args) = parser.parse_args()
    feed_utils.VERBOSE = options.verbose
    args_utils.require_args_length(parser, args, 2)

    args_utils.require_arg_number(parser, args, 0, "nels_id")
    config.init()

    proot = path.join(storage_facade.USERS_ROOT_DIR, user_facade.nels_id_to_username(int(args[0])), "Projects")
    plink = path.join(proot, args[1])
    if not path.exists(plink):
        run_utils.exit_fail("symbolic link not found")

    # unlock folder, remove link and lock folder again
    run_utils.launch_cmd("/bin/chflags -h nosimmutable,nosunlink %s" % proot)
    run_utils.launch_cmd("/bin/rm -f %s " % plink)
    run_utils.launch_cmd("/bin/chflags -h simmutable,sunlink %s" % proot)

    #verify
    if path.exists(plink):
        feed_utils.failed("link still exists")
    else:
        feed_utils.ok("link removed successfully")
