# -- coding: utf-8 --


from optparse import OptionParser
import config

from api import sbi_project
from utils import feed_utils, run_utils


def test_recompute_disc_usage():
    feed_utils.heading("Trying recompute sbi project disc usage")
    success = sbi_project.recompute_project_disc_usage()
    if success is False:
        feed_utils.failed("recompute failed")
    else:
        feed_utils.ok("recompute success")

    return success


if __name__ == "__main__":
    parser = OptionParser(
        usage='usage: %prog [-v] [-n] ')
    parser.add_option('-v', '--verbose', dest="verbose", action="store_true", help='turn verbosity on', default=False)
    parser.add_option('-n', '--notify', dest="notify", action="store_true", help='turn notification on', default=False)

    (options, args) = parser.parse_args()
    feed_utils.VERBOSE = options.verbose
    feed_utils.NOTIFY = options.notify

    success = test_recompute_disc_usage()
    if success:
        run_utils.exit_ok("", "sbi recompute project disc usage integration test: ok")
    run_utils.exit_fail("", "sbi recompute project disc usage integration test: failed")
