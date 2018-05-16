# -*- coding: utf-8 -*-


from optparse import OptionParser

import config
from utils import feed_utils, args_utils, ssh_utils, run_utils, file_utils
from api import job


def test_copy_nels_to_sbi(nels_id):
    feed_utils.heading("copy use case nels to sbi. nels_id: %s" % nels_id)

    credentail = ssh_utils.get_ssh_credentials(nels_id)
    if not credentail:
        feed_utils.failed("fetching key")
        return False
    feed_utils.ok("ssh key fetched")
    (host, username, key_file) = credentail

    test_file = "dummy5.txt"

    src_dir = "Personal"
    src_file = "%s/%s" % (src_dir, test_file)
    dest_dir = ''

    if not ssh_utils.write_dummy_file(key_file, username, host, src_file, "5k"):  # 5M
        feed_utils.failed("write test file")
        return False
    feed_utils.ok("write test file")

    dataset_id = "c16ff090-605d-4228-a8c9-3713a9a40e45"
    dataset_name = "regtest1feb"
    subtype = "Intensities"
    subtype_id = 1124874

    job_id = job.add_sbi_job(job.SBI_PULL, nels_id, [test_file], [], host, username,
                             file_utils.read_file_content(key_file), dataset_id, dataset_name, subtype, subtype_id,
                             src_dir, "")

    if job_id == -1:
        feed_utils.failed("job submission")
        return False
    feed_utils.ok("job submission. job-id: %s" % job_id)
    job.wait_for_job(job_id)
    # validate file (to do)

    # clean up
    if not job.delete_job(job_id):
        feed_utils.failed("job delete")
        return False
    feed_utils.ok("job delete")

    # clean up on NeLS side
    [exit_code, output, error] = run_utils.launch_remote_with_key(key_file, username, host, "rm -rf %s " % (src_file))
    if exit_code != 0:
        feed_utils.error(error)
        return False
    feed_utils.ok("clean up")
    return True


if __name__ == "__main__":
    parser = OptionParser(usage='usage: %prog nels_id')
    parser.add_option('-v', '--verbose', dest="verbose", action="store_true", help='turn verbosity on', default=False)
    (options, args) = parser.parse_args()
    feed_utils.VERBOSE = options.verbose

    args_utils.require_args_length(parser, args, 1)
    args_utils.require_arg_number(parser, args, 0, 'nels_id')
    test_copy_nels_to_sbi(int(args[0]))
