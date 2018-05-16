__author__ = 'Xiaxi Li'
__email__ = 'xiaxi.li@ii.uib.no'
__date__ = '10/Apr/2017'

from optparse import OptionParser

import config
from user import test_users_list
from project import test_projects_list
from api import users, project
from utils import feed_utils, args_utils, mail_utils, run_utils

if __name__ == "__main__":
    parser = OptionParser(usage='usage: %prog [-v] email project_name')
    parser.add_option('-v', '--verbose', dest="verbose", action="store_true", help='turn verbosity on', default=False)
    parser.add_option('-n', '--notify', dest="notify", action="store_true", help='turn notification on', default=False)
    (options, args) = parser.parse_args()
    config.VERBOSE = options.verbose
    feed_utils.NOTIFY = options.notify

    args_utils.require_args_length(parser, args, 2)
    email = str(args[0])
    if not mail_utils.is_valid_email(email):
        run_utils.exit_fail("invalid e-mail")

    project_name = str(args[1])

    nels_id = users.register_user(users.NELS_IDP, email, "Test-added-user", email, users.NORMAL_USER, True, "TEST")
    if not nels_id:
        run_utils.exit_fail("user registration failed")
    test_users_list.test_user_display(nels_id)

    project_id = project.add_project(project_name, 'added from integration test')
    if not project_id:
        feed_utils.failed("project creation failed")
    else:
        test_projects_list.test_project_display(project_id)

        default_membership_type = 1
        if project.add_user_to_project(project_id, nels_id, default_membership_type):
            feed_utils.ok("user was added to project")
            if not default_membership_type == project.get_user_in_project(project_id, nels_id):
                feed_utils.failed("the user's membership type is not correct")
            else:
                feed_utils.ok("the user's membership type is correct")
        else:
            feed_utils.failed("user was not added to project")

        if not project.delete_project(project_id):
            feed_utils.failed("unable to delete project")
        else:
            feed_utils.ok("project deleted successfully")

    if not users.delete_user(nels_id):
        run_utils.exit_fail("uanble to delete user")
    run_utils.exit_ok("user deleted successfully")