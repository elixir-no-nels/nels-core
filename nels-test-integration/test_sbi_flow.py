# -- coding: utf-8 --

import sys
from optparse import OptionParser

from test_sbi_project_add import test_project_add
from test_sbi_project_add_user import test_project_add_member
from test_sbi_project_del import test_project_del
from test_sbi_project_remove_user import test_project_delete_member
from test_sbi_quota_add import test_quota_add
from test_sbi_quota_del import test_quota_del
from test_sbi_quotas_search import test_quota_search
from utils import feed_utils, args_utils, run_utils

if __name__ == "__main__":
    parser = OptionParser(
        usage='usage: %prog [-v] [-n] quota_name, project_name  federated_id ')
    parser.add_option('-v', '--verbose', dest="verbose", action="store_true", help='turn verbosity on', default=False)
    parser.add_option('-n', '--notify', dest="notify", action="store_true", help='turn notification on', default=False)

    (options, args) = parser.parse_args()
    feed_utils.VERBOSE = options.verbose
    feed_utils.NOTIFY = options.notify

    args_utils.require_args_length(parser, args, 3)
    quota_name = str(args[0])
    project_name = str(args[1])
    federated_id = str(args[2])

    success = test_quota_add(quota_name, federated_id)

    ids = test_quota_search(quota_name)

    success = success and len(ids) == 2

    if success:
        project_id = test_project_add(ids[1], project_name, federated_id)

        if project_id is not None:
            test_project_add_member(project_id, 1, federated_id)

            test_project_delete_member(project_id, federated_id)

            test_project_del(federated_id, project_id)

        deleted = test_quota_del(ids[0])

        if deleted:
            sys.exit(0)
    run_utils.exit_fail("", "sbi integration failed")
