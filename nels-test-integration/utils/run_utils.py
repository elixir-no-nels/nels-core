'''
Created on Feb 18, 2017

@author: kidane
'''

import sys, subprocess, os

from utils import feed_utils, mail_utils


def exit_fail(msg="",email_subject="integration test failed"):
    feed_utils.failed(msg)
    if feed_utils.NOTIFY:
        mail_utils.send_mail(feed_utils.NOTIFY_TO, email_subject, feed_utils.COLLECTED_FEED, feed_utils.NOTIFY_FROM)
    sys.exit(-1)

def exit_ok(msg="",email_subject="integration test ok"):
    feed_utils.ok(msg)
    if feed_utils.NOTIFY:
        mail_utils.send_mail(feed_utils.NOTIFY_TO, email_subject, feed_utils.COLLECTED_FEED, feed_utils.NOTIFY_FROM)
    sys.exit(0)

def launch_cmd(cmd, as_user=''):
    effective_command = cmd
    if not as_user == '':
        effective_command = "su - %s -c '%s'" % (as_user, cmd.replace("'", "\'").replace(" ", "\ "))
    feed_utils.info(effective_command)  
    if os.name != 'nt':
        process = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True)
    else:
        startupinfo = subprocess.STARTUPINFO()
        startupinfo.dwFlags |= subprocess.STARTF_USESHOWWINDOW
        process = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True, startupinfo=startupinfo)
    
    (output, err) = process.communicate()
    exit_code = process.wait()
    result = [exit_code, output, err]
    feed_utils.info(result)
    return result

def launch_remote(username, server, cmd):
    launch_cmd(" ssh %s@%s '%s'" % (username, server, cmd))

def launch_remote_with_key(key_file, username, host, cmd):
    return launch_cmd("ssh -i %s %s@%s  '%s'" % (key_file, username, host, cmd))
