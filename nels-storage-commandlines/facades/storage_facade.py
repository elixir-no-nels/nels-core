import grp

from utils import feed_utils, run_utils

PROJECTS_ROOT_DIR = ''
USERS_ROOT_DIR = ''


def nels_id_to_sys_id(nels_id):
    return ("{0:x}".format(int(nels_id) + 4200))


def hex_to_dec(hex_string):
    return int(hex_string, 16)


def sys_to_nels_id(sys_id):
    return hex_to_dec(sys_id) - 4200


def username_to_nels_id(username):
    hex_id = username[1:]  # since first character is "u"
    return sys_to_nels_id(hex_id)


def file_owner_get(directory):
    cmd = "stat -f '%Su' " + directory
    feed_utils.info(cmd)
    result = run_utils.launch_cmd(cmd)
    # caution: this is not safe. Should be changed
    return result[1][0].replace('\n', '')


def permissions_strict_owner_only(directory):
    owner = file_owner_get(directory)
    setfacl = '/bin/setfacl'
    chmod = '/bin/chmod'
    if owner == "root":
        owner = ''
    run_utils.launch_cmd("%s -h -b %s" % (setfacl, directory), owner)
    run_utils.launch_cmd("%s 700 %s" % (chmod, directory), owner)
    run_utils.launch_cmd("%s -h -m %s %s" % (setfacl, "'owner@:rwxp--aARWcCos:fd:allow'", directory), owner)


def allow_nels_storage_admin(directory):
    run_utils.launch_cmd("/bin/setfacl -h -a2 %s %s" % ("'g:nels_storage_admin:xaRcs:fd:allow'", directory))


def permissions_add_full_control_to_groupname(directory, group_name):
    run_utils.launch_cmd("/bin/setfacl -h -m 'g:%s:rwxpDdaARWcC:fd:allow' %s" % (group_name, directory))


def group_exists(grp):
    result = run_utils.launch_cmd("/usr/sbin/pw groupshow %s" % grp)
    return result[0] == 0


def member_nels_ids(group_name):
    nels_ids = []
    try:
        for username in grp.getgrnam(group_name).gr_mem:
            try:
                nels_ids.append(username_to_nels_id(username))
            except:
                continue
    except:
        pass
    return nels_ids
