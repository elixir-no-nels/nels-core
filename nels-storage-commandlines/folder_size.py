import json
import os
from optparse import OptionParser
from os import path

import config
from utils import feed_utils, args_utils, run_utils, file_utils


def run_for_self(folder_path):
    return file_utils.get_tree_size(folder_path)
    '''
    try:
        cmd = "du -s %s " % folder_path
        (status, output) = run_utils.launch_cmd(cmd)
        if not status == 0:
            run_utils.exit_fail(output[0])
        run_utils.exit_ok(string_utils.left_of_first_occurance(output[0], "\t"))
    except Exception as ex:
        run_utils.exit_fail(ex)
    '''


def run_for_children(folder_path):
    try:
        ret = {}
        for child in os.listdir(folder_path):
            pth = path.join(folder_path, child)
            if path.isdir(pth):
                ret[child] = file_utils.get_tree_size(pth)
        run_utils.exit_ok(json.dumps(ret, ensure_ascii=False, encoding='utf8'))
        '''
        cmd = "du -d 1 %s " % folder_path
        (status, output) = run_utils.launch_cmd(cmd)
        if not status == 0:
            run_utils.exit_fail(output[0])

        for line in output[0].split("\n"):
            if "\t" not in line:
                continue
            [size, pth] = line.split("\t")
            if not pth == folder_path:
                ret[pth.replace(folder_path, "").replace("/","")] = int(size)
        run_utils.exit_ok(json.dumps(ret, ensure_ascii=False, encoding='utf8'))
        '''
    except Exception as ex:
        run_utils.exit_fail(ex)


if __name__ == "__main__":
    usage = 'usage: %prog [options]'
    version = '%prog 1.0'

    parser = OptionParser(usage=usage, version=version)
    parser.add_option('-v', '--verbose', dest="verbose", action="store_true", help='turn verbosity on', default=False)
    parser.add_option('-c', '--children', dest="children", action="store_true", help='return size of children folders',
                      default=False)
    # get options and arguments
    (options, args) = parser.parse_args()
    feed_utils.VERBOSE = options.verbose
    args_utils.require_args_length(parser, args, 1)

    folder_path = args[0]

    config.init()
    if not path.exists(folder_path):
        run_utils.exit_fail("folder not found: %s" % folder_path)

    if not path.isdir(folder_path):
        run_utils.exit_fail("path not a folder: %s" % folder_path)

    if options.children:
        run_for_children(folder_path)
    else:
        run_for_self(folder_path)
