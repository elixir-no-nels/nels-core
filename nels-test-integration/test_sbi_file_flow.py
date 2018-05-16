# -- coding: utf-8 --

import sys
from optparse import OptionParser
import config

from utils import feed_utils, args_utils, run_utils
from test_copy_nels_to_sbi import test_copy_nels_to_sbi
from test_copy_sbi_to_nels import test_copy_sbi_to_nels

if __name__ == "__main__":
    parser = OptionParser(
        usage='usage: %prog [-v] [-n] nels_id ')
    parser.add_option('-v', '--verbose', dest="verbose", action="store_true", help='turn verbosity on', default=False)
    parser.add_option('-n', '--notify', dest="notify", action="store_true", help='turn notification on', default=False)

    (options, args) = parser.parse_args()
    feed_utils.VERBOSE = options.verbose
    feed_utils.NOTIFY = options.notify

    args_utils.require_args_length(parser, args, 1)
    args_utils.require_arg_number(parser, args, 0, 'nels_id')

    success = test_copy_nels_to_sbi(int(args[0]))

    success = success and test_copy_sbi_to_nels(int(args[0]))

    if success:
        sys.exit(0)
    run_utils.exit_fail("", "sbi file transfer integration test: failed")
