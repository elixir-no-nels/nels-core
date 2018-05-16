# -- coding: utf-8 --

from optparse import OptionParser

import config
from api import sbi_quotas
from utils import feed_utils, args_utils


def test_quota_search(name):
    feed_utils.heading("Trying quota search")
    result = sbi_quotas.search_quotas(name)
    if not result:
        feed_utils.failed("search quotas failed")
    else:
        feed_utils.ok("found %s quotas from the search" % result[u'count'])
        for quota in result[u'data']:

            feed_utils.info("id: %d, quota_id: %d" % (int(quota[u'id']), int(quota[u'quota_id'])))

            if result[u'count'] == 1: return int(quota[u'id']), int(quota[u'quota_id'])
    return None


if __name__ == "__main__":
    parser = OptionParser(usage='usage: %prog [-v] query')
    parser.add_option('-v', '--verbose', dest="verbose", action="store_true", help='turn verbosity on', default=False)

    (options, args) = parser.parse_args()
    feed_utils.VERBOSE = options.verbose

    args_utils.require_args_length(parser, args, 1)
    query = str(args[0])

    test_quota_search(query)
