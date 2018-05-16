__author__ = 'Xiaxi Li'
__email__ = 'xiaxi.li@ii.uib.no'
__date__ = '14/Mar/2017'

from optparse import OptionParser

import config
from api import project
from utils import feed_utils, args_utils


def test_project_del(project_id):
    feed_utils.heading("Trying project deletion. project_id : %s" % project_id)
    if not project.delete_project(project_id):
        feed_utils.failed("deletion of project failed")
    else:
        feed_utils.ok("deletion of project")


if __name__ == "__main__":
    parser = OptionParser(usage='usage: %prog [-v] project_id')
    parser.add_option('-v', '--verbose', dest="verbose", action="store_true", help='turn verbosity on', default=False)
    (options, args) = parser.parse_args()
    feed_utils.VERBOSE = options.verbose

    args_utils.require_args_length(parser, args, 1)
    args_utils.require_arg_number(parser, args, 0, 'project_id')
    project_id = int(args[0])
    test_project_del(project_id)
