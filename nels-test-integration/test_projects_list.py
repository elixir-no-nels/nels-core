__author__ = 'Xiaxi Li'
__email__ = 'xiaxi.li@ii.uib.no'
__date__ = '14/Mar/2017'

import random
from optparse import OptionParser

import config
from utils import feed_utils
from api import project


def test_project_display(project_id):
    feed_utils.heading("Trying project display. project_id: %s" % project_id)
    project_info = project.get_project(project_id)
    if not project_info:
        feed_utils.failed("failed getting project details")
    else:
        feed_utils.ok("project details: %s" % project_info)


def test_projects_list():
    feed_utils.heading("Trying projects list")
    project_ids = project.get_project_ids()
    if not project_ids:
        feed_utils.failed("get project ids")
        return False
    feed_utils.ok("fetched %s project ids" % len(project_ids))

    random_project_id = project_ids[random.randrange(0, len(project_ids) - 1)]
    test_project_display(random_project_id)
    return True


if __name__ == "__main__":
    parser = OptionParser(usage='usage: %prog project_id')
    parser.add_option('-v', '--verbose', dest="verbose", action="store_true", help='turn verbosity on', default=False)
    (options, args) = parser.parse_args()
    feed_utils.VERBOSE = options.verbose
    test_projects_list()
