# -- coding: utf-8 --

from optparse import OptionParser

from api import sbi_users
from utils import feed_utils


def test_users_list():
    feed_utils.heading("Trying sbi user ids")
    user_ids = sbi_users.get_user_ids()
    if not user_ids:
        feed_utils.failed("get user ids")
        return

    for user_id in user_ids:
        feed_utils.info(user_id)

    feed_utils.ok("")


if __name__ == "__main__":
    parser = OptionParser(usage='usage: %prog ')
    parser.add_option('-v', '--verbose', dest="verbose", action="store_true", help='turn verbosity on', default=False)
    (options, args) = parser.parse_args()
    feed_utils.VERBOSE = options.verbose
    test_users_list()
