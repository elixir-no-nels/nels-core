__author__ = 'Xiaxi Li'
__email__ = 'xiaxi.li@ii.uib.no'
__date__ = '08/Apr/2017'

from optparse import OptionParser

import config
import test_projects_list
from api import project
from utils import args_utils, run_utils

if __name__ == "__main__":
    parser = OptionParser(usage='usage: %prog name')
    parser.add_option('-v', '--verbose', dest="verbose", action="store_true", help='turn verbosity on', default=False)
    (options, args) = parser.parse_args()
    feed_utils.VERBOSE = options.verbose

    args_utils.require_args_length(parser, args, 1)
    name = str(args[0])

    project_id = project.add_project(name, 'added from integration test')
    if not project_id:
        run_utils.exit_fail("project creation failed")

    test_projects_list.test_project_display(project_id)
    if not project.delete_project(project_id):
        run_utils.exit_fail("unable to delete project")
    run_utils.exit_ok("project deleted successfully")
