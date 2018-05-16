from os import path

PROJECTS_ROOT_DIR = ''
USERS_ROOT_DIR = ''


def get_project_home(project_id):
    return path.join(PROJECTS_ROOT_DIR, project_id_to_name(project_id))


def get_user_home(nels_id):
    return path.join(USERS_ROOT_DIR, nels_id_to_username(nels_id))


def nels_id_to_username(nels_id):
    return ("u%s" % nels_id_to_sys_id(nels_id))


def project_id_to_name(pid):
    return ("p%s" % nels_id_to_sys_id(pid))


def nels_id_to_sys_id(nels_id):
    return ("{0:x}".format(int(nels_id) + 4200))
