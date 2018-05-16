# -- coding: utf-8 --

from optparse import OptionParser

from api import sbi_project
from utils import feed_utils, args_utils


def test_project_del(federated_id, project_id):
    feed_utils.heading("Trying project deletion. project id: %s" % project_id)
    if not sbi_project.delete_project(federated_id, project_id):
        feed_utils.failed("deletion of project failed")
    else:
        feed_utils.ok("deletion of project")


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

    test_project_del(federated_id, project_id)
