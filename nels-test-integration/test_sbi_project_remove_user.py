# -- coding: utf-8 --

from optparse import OptionParser

from api import sbi_project
from utils import feed_utils, args_utils


def test_project_delete_member(project_id, federated_id):
    feed_utils.heading("Trying delete member from project")
    success = sbi_project.delete_user_from_project(project_id, federated_id)
    if success != True:
        feed_utils.failed("delete member from project failed")
    else:
        feed_utils.ok("removed member in project. ")

    return project_id


if __name__ == "__main__":
    parser = OptionParser(
        usage='usage: %prog [-v]  project_id federated_id ')
    parser.add_option('-v', '--verbose', dest="verbose", action="store_true", help='turn verbosity on', default=False)

    (options, args) = parser.parse_args()
    feed_utils.VERBOSE = options.verbose

    args_utils.require_args_length(parser, args, 2)

    args_utils.require_arg_number(parser, args, 0, "project_id")

    project_id = int(args[0])
    federated_id = str(args[1])

    test_project_delete_member(project_id, federated_id)
