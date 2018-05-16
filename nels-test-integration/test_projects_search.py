__author__ = 'Xiaxi Li'
__email__ = 'xiaxi.li@ii.uib.no'
__date__ = '15/Mar/2017'

from optparse import OptionParser

import config
import test_projects_list
from api import project
from utils import feed_utils, run_utils, string_utils


def test_project_search(project_id=None, name=None):
    result = project.search_projects(project_id, name)
    if not result:
        feed_utils.failed("search projects failed")
    else:
        feed_utils.ok("found %s projects from the search" % len(result))
        for proj in result:
            test_projects_list.test_project_display(proj['id'])


if __name__ == "__main__":
    parser = OptionParser(
        usage='usage: %prog [-v] [-i project_id] [-n name]  : Note - at least one of the filters should be provided')
    parser.add_option('-v', '--verbose', dest="verbose", action="store_true", help='turn verbosity on', default=False)
    parser.add_option('-i', '--nels_id', dest="project_id", help='the project id')
    parser.add_option('-n', '--name', dest="name", help='project name')
    (options, args) = parser.parse_args()
    feed_utils.VERBOSE = options.verbose

    project_id = None
    name = None

    if options.project_id:
        if not string_utils.is_number(options.project_id):
            run_utils.exit_fail("project_id should be number")
        project_id = int(options.project_id)

    if options.name:
        name = options.name

    if not project_id and not name:
        parser.print_usage()
        run_utils.exit_fail("at least one search parameter should be provided")

    test_project_search(project_id=project_id, name=name)

