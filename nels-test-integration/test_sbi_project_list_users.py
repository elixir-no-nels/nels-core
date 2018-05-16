# -- coding: utf-8 --

from optparse import OptionParser
import config
from api import sbi_project
from utils import feed_utils, args_utils


def test_project_list_all_users(project_id):
    feed_utils.heading("Trying list users in project. project_id: %s" % project_id)
    project_info = sbi_project.get_users_in_project(project_id)
    if project_info == None:
        feed_utils.failed("failed getting project users")

    elif project_info == []:
        feed_utils.ok("project without users: %s" % project_info)

    else:
        feed_utils.ok("number of project users: %s" % len(project_info))

        for user in project_info:
            feed_utils.info("federated_id : %s" % user[u'federated_id'])


if __name__ == "__main__":
    parser = OptionParser(
        usage='usage: %prog [-v]  project_id ')
    parser.add_option('-v', '--verbose', dest="verbose", action="store_true", help='turn verbosity on', default=False)

    (options, args) = parser.parse_args()
    feed_utils.VERBOSE = options.verbose

    args_utils.require_args_length(parser, args, 1)

    args_utils.require_arg_number(parser, args, 0, "project_id")

    project_id = int(args[0])

    test_project_list_all_users(project_id)
