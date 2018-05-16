'''
Created on Mar 10, 2017

@author: kidane
'''
import string_utils

VERBOSE = False
TAB_COUNT = 0

# notification related
NOTIFY = False
NOTIFY_FROM = 'kidane.tekle@uib.no'
NOTIFY_TO = ['kidane.tekle@uib.no']
COLLECTED_FEED = ''


def push_in():
    global TAB_COUNT
    TAB_COUNT += 1


def push_out():
    global TAB_COUNT
    TAB_COUNT -= 1


def get_tabs():
    return "\t" * TAB_COUNT if TAB_COUNT > 0 else ""


def prefixed_message(decoration, msg):
    return "%s. %s" % (decoration, str(msg)) if TAB_COUNT == 0 else "%s %s" % (get_tabs(), str(msg))

def ok(msg):
    ok_msg = prefixed_message("ok", msg)
    print (ok_msg)
    handle_notify(ok_msg)


def failed(msg):
    failed_msg = prefixed_message("failed", msg)
    print (failed_msg)
    handle_notify(failed_msg)


def heading(msg):
    heading_msg = "======================================\n%s\n======================================" % msg
    print (heading_msg)
    handle_notify(heading_msg)


def info(msg):
    if VERBOSE:
        info_msg = prefixed_message("info", msg)
        print (info_msg)
        handle_notify(info_msg)


def error(msg):
    error_msg = prefixed_message("error", msg)
    print(error_msg)
    handle_notify(error_msg)


def debug(msg):
    if VERBOSE:
        debug_msg = prefixed_message("debug", msg)
        print (debug_msg)
        handle_notify(debug_msg)


def handle_notify(msg):
    global NOTIFY, COLLECTED_FEED
    if NOTIFY:
        COLLECTED_FEED = string_utils.append_with_delimiter(COLLECTED_FEED, msg, "\n", "", "")
