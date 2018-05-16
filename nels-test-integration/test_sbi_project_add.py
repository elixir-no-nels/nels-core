# -- coding: utf-8 --

from optparse import OptionParser

from api import sbi_project
from utils import feed_utils, args_utils


def test_project_add(quota_id, name, federated_id):
    feed_utils.heading("Trying project creation")
    project_id = sbi_project.add_project(quota_id, name, federated_id)
    if project_id is None:
        feed_utils.failed("project creation failed")
    else:
        feed_utils.ok("new created project. project id: %s " % project_id)

    return project_id


if __name__ == "__main__":
    parser = OptionParser(
        usage='usage: %prog [-v]  quota_id name federated_id ')
    parser.add_option('-v', '--verbose', dest="verbose", action="store_true", help='turn verbosity on', default=False)

    (options, args) = parser.parse_args()
    feed_utils.VERBOSE = options.verbose

    args_utils.require_args_length(parser, args, 3)

    args_utils.require_arg_number(parser, args, 0, "quota_id")

    quota_id = int(args[0])
    name = str(args[1])
    federated_id = str(args[2])

    project_id = test_project_add(quota_id, name, federated_id)
