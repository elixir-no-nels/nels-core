# -- coding: utf-8 --

from optparse import OptionParser
import config

from utils import feed_utils, args_utils
from api import sbi_quotas


def test_quota_del(id):
    feed_utils.heading("Trying quota deletion. id : %s" % id)
    if not sbi_quotas.delete_quota(id):
        feed_utils.failed("deletion of quota failed")
        return False

    feed_utils.ok("deletion of quota")
    return True


if __name__ == "__main__":
    parser = OptionParser(usage='usage: %prog [-v]  id')
    parser.add_option('-v', '--verbose', dest="verbose", action="store_true", help='turn verbosity on', default=False)

    (options, args) = parser.parse_args()
    feed_utils.VERBOSE = options.verbose

    args_utils.require_args_length(parser, args, 1)
    args_utils.require_arg_number(parser, args, 0, "id")
    id = int(args[0])

    test_quota_del(id)
