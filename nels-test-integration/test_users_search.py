from optparse import OptionParser

import test_users_list
from api import users
from utils import feed_utils, run_utils, string_utils


def test_users_search(nels_id=None, idp_number=None, federated_id=None, name=None, email=None, user_type_id=None,
                      is_active=None, affiliation=None):
    result = users.search_users(nels_id, idp_number, federated_id, name, email, user_type_id, is_active, affiliation)
    if not result:
        feed_utils.failed("search users failed")
    else:
        feed_utils.ok("found %s users from the search" % len(result))
        for usr in result:
            test_users_list.test_user_display(usr['id'])


if __name__ == "__main__":
    parser = OptionParser(
        usage='usage: %prog [-v] [-i nels_id] [-n name] [-e email]  : Note - at least one of the filters should be provided')
    parser.add_option('-v', '--verbose', dest="verbose", action="store_true", help='turn verbosity on', default=False)
    parser.add_option('-i', '--nels_id', dest="nels_id", help='the nels id')
    parser.add_option('-e', '--email', dest="email", help='user email')
    parser.add_option('-n', '--name', dest="name", help='user name')
    (options, args) = parser.parse_args()
    feed_utils.VERBOSE = options.verbose

    nels_id = None
    email = None
    name = None

    if options.nels_id:
        if not string_utils.is_number(options.nels_id):
            run_utils.exit_fail("nels_id should be number")
        nels_id = int(options.nels_id)

    if options.email:
        email = options.email

    if options.name:
        name = options.name

    if not nels_id and not email and not name:
        parser.print_usage()
        run_utils.exit_fail("at least one search parameter should be provided")

    test_users_search(nels_id=nels_id, email=email, name=name)
