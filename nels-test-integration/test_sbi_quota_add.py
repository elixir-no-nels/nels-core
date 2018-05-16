# -- coding: utf-8 --


from optparse import OptionParser

from api import sbi_quotas
from utils import feed_utils, args_utils


def test_quota_add(name, federated_id):
    feed_utils.heading("Trying quota creation")
    created_code = sbi_quotas.add_quota(name, federated_id)
    if created_code == None:
        feed_utils.failed("quota creation failed")
        return False

    feed_utils.ok("new created quota ")
    return True


if __name__ == "__main__":
    parser = OptionParser(
        usage='usage: %prog [-v]  name  federated_id ')
    parser.add_option('-v', '--verbose', dest="verbose", action="store_true", help='turn verbosity on', default=False)

    (options, args) = parser.parse_args()
    feed_utils.VERBOSE = options.verbose

    args_utils.require_args_length(parser, args, 2)
    name = str(args[0])
    federated_id = str(args[1])

    test_quota_add(name, federated_id)
