import sys, subprocess

import feed_utils,mail_utils


def exit_fail(msg=""):
    if not msg == "":
        feed_utils.failed(msg)
    if feed_utils.NOTIFY:
        mail_utils.send_mail(feed_utils.NOTIFY_TO, "app failure", feed_utils.COLLECTED_FEED,feed_utils.NOTIFY_FROM)
    sys.exit(-1)

def exit_ok(msg=""):
    if not msg == "":
        feed_utils.ok(msg)
    if feed_utils.NOTIFY:
        mail_utils.send_mail(feed_utils.NOTIFY_TO, "TC app ok", feed_utils.COLLECTED_FEED,feed_utils.NOTIFY_FROM)
    sys.exit(0)

def launch_cmd(cmd, as_user=''):
    effective_command = cmd
    if not as_user == '':
        effective_command = "su - %s -c '%s'" % (as_user, cmd.replace("'", "\'").replace(" ","\ "))
    feed_utils.info(effective_command)
    p = subprocess.Popen(effective_command, stdout=subprocess.PIPE, shell=True, stderr=subprocess.PIPE, bufsize=1)
    output = p.communicate()
    p_status = p.wait()
    result = (p_status, output)
    if int(result[0]) == 0:
        feed_utils.info(result)
    else:
        feed_utils.error(result)

def launch_remote(username, server, cmd):
    launch_cmd(" ssh %s@%s '%s'" %(username,server,cmd))