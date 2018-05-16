from optparse import OptionParser

import test_users_list
from api import users
from utils import feed_utils, args_utils, mail_utils, run_utils


def test_user_registration(idp_number, federated_id, name, email, user_type_id, is_active, affiliation):
    feed_utils.heading("Trying user registration")
    new_id = users.register_user(idp_number, federated_id, name, email, user_type_id, is_active, affiliation)
    if not new_id:
        feed_utils.failed("user registration failed")
    else:
        feed_utils.ok("new user registered. nels_id: %s " % new_id)
        test_users_list.test_user_display(new_id)


if __name__ == "__main__":
    parser = OptionParser(usage='usage: %prog email')
    parser.add_option('-v', '--verbose', dest="verbose", action="store_true", help='turn verbosity on', default=False)
    (options, args) = parser.parse_args()
    feed_utils.VERBOSE = options.verbose

    args_utils.require_args_length(parser, args, 1)
    email = str(args[0])
    if not mail_utils.is_valid_email(email):
        run_utils.exit_fail("invalid e-mail")
    test_user_registration(users.NELS_IDP, email, "Test-added-user", email, users.NORMAL_USER, True, "TEST")
