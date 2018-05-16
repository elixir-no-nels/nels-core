from optparse import OptionParser

import config
from facades import user_facade, project_facade
from utils import feed_utils, args_utils, run_utils

if __name__ == "__main__":
    usage = 'usage: %prog [options] project_id'
    version = '%prog 1.0'

    parser = OptionParser(usage=usage, version=version)
    parser.add_option('-v', '--verbose', dest="verbose", action="store_true", help='turn verbosity on', default=False)
    # get options and arguments
    (options, args) = parser.parse_args()
    feed_utils.VERBOSE = options.verbose
    args_utils.require_args_length(parser, args, 1)

    args_utils.require_arg_number(parser, args, 0, "project_id")
    pid = args[0]

    config.init()
    feed_utils.heading("project info")

    if not project_facade.project_exists(pid):
        run_utils.exit_fail("project not found")

    feed_utils.ok("project found\nname:\t%s\nhome folder:\t%s" % (project_facade.project_name_by_id(pid),
                                                                  project_facade.project_home(pid)))

    feed_utils.push_in()
    for role in [project_facade.MembershipRoles.ADMIN, project_facade.MembershipRoles.POWERUSER,
                 project_facade.MembershipRoles.MEMBER]:
        feed_utils.push_in()
        members = project_facade.project_members(pid, role)
        feed_utils.info("role:\t%s (members count: %s)" % (role, len(members)))
        for uid in members:
            feed_utils.info("\tnels_id:\t%s\tusername:\t%s" % (uid,
                                                               user_facade.nels_id_to_username(uid)))
        feed_utils.push_out()
