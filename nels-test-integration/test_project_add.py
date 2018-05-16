__author__ = 'Xiaxi Li'
__email__ = 'xiaxi.li@ii.uib.no'
__date__ = '14/Mar/2017'

from optparse import OptionParser

import config
from api import project
from utils import feed_utils, args_utils

from test_projects_list import test_project_display


def test_project_add(name, description):
    feed_utils.heading("Trying project creation")
    new_id = project.add_project(name, description)
    if not new_id:
        feed_utils.failed("project creation failed")
    else:
        feed_utils.ok("new created prject. project_id: %s " % new_id)
        test_project_display(new_id)


if __name__ == "__main__":
    parser = OptionParser(usage='usage: %prog name')
    parser.add_option('-v', '--verbose', dest="verbose", action="store_true", help='turn verbosity on', default=False)
    (options, args) = parser.parse_args()
    feed_utils.VERBOSE = options.verbose

    args_utils.require_args_length(parser, args, 1)
    name = str(args[0])
    test_project_add(name, 'added from integration test')
