from optparse import OptionParser

import config
from facades import storage_facade, disk_facade
from nels_master_api import stats, projects
from utils import feed_utils

if __name__ == "__main__":
    usage = 'usage: %prog [options]'
    version = '%prog 1.0'

    parser = OptionParser(usage=usage, version=version)
    parser.add_option('-v', '--verbose', dest="verbose", action="store_true", help='turn verbosity on', default=False)
    parser.add_option('-d', '--dryrun', dest="dryrun", action="store_true", help='Dry run, don''t affect persistence',
                      default=False)
    # get options and arguments
    (options, args) = parser.parse_args()
    config.DRY_RUN = options.dryrun
    feed_utils.VERBOSE = options.verbose

    config.init()
    feed_utils.heading("running project disk usage statistics")
    total_size = 0
    for project_id in projects.get_project_ids():
        try:
            size = disk_facade.get_folder_size(storage_facade.get_project_home(project_id))
            total_size = total_size + size
            feed_utils.info("%s:%s" % (project_id, size))
            if not config.DRY_RUN:
                stats.add_stat(stats.PROJECT_DISK_USAGE, project_id, size)
        except Exception as ex:
            feed_utils.error("error processing project id: %s" % project_id)
    if not config.DRY_RUN:
        stats.add_stat(stats.PROJECT_DISK_USAGE_SUMMARY, -1,total_size)