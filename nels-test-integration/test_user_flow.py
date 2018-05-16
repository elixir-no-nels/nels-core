'''
Created on Feb 28, 2017

@author: kidane
'''

from optparse import OptionParser

import config
import test_users_list
from api import users
from test_copy import test_copy
from test_move import test_move
from test_ssh import test_ssh
from utils import feed_utils, args_utils, mail_utils, run_utils

if __name__ == "__main__":
    parser = OptionParser(usage='usage: %prog [-v] email')
    parser.add_option('-v', '--verbose', dest="verbose", action="store_true", help='turn verbosity on', default=False)
    parser.add_option('-n', '--notify', dest="notify", action="store_true", help='turn notification on', default=False)
    (options, args) = parser.parse_args()
    feed_utils.VERBOSE = options.verbose
    feed_utils.NOTIFY = options.notify

    args_utils.require_args_length(parser, args, 1)
    email = str(args[0])
    if not mail_utils.is_valid_email(email):
        run_utils.exit_fail("invalid e-mail")

    # list users
    users_list_ok = test_users_list.test_users_list()
    feed_utils.heading("A full life-cyle of user")
    nels_id = users.register_user(users.NELS_IDP, email, "Test-added-user", email, users.NORMAL_USER, True, "TEST")
    if not nels_id:
        run_utils.exit_fail("user registration failed")

    test_users_list.test_user_display(nels_id)
    ssh_ok = test_ssh(nels_id)
    if ssh_ok:
        test_copy(nels_id)
        test_move(nels_id)
    if not users.delete_user(nels_id):
        run_utils.exit_fail("uanble to delete user")
    feed_utils.ok("user deleted successfully")
