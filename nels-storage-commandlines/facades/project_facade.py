import os
from os import path

from facades import user_facade, storage_facade
from utils import run_utils, feed_utils


class MembershipRoles():
    POWERUSER = 'poweruser'
    ADMIN = 'admin'
    MEMBER = 'member'


def project_id_to_name(pid):
    return ("p%s" % storage_facade.nels_id_to_sys_id(pid))


def role_to_group(pid, role):
    grps = {MembershipRoles.MEMBER: "", MembershipRoles.POWERUSER: "-p", MembershipRoles.ADMIN: "-a"}
    return "%s%s" % (project_id_to_name(pid), grps[role])


def project_exists(pid):
    feed_utils.info("cheking project existence. pid: %s" % pid)
    return run_utils.launch_cmd('/usr/bin/getent group %s' % project_id_to_name(pid))[0] == 0


def cleanup_project_name(pname):
    return pname.replace(" ", "-")


def set_project_name(project_home, name):
    run_utils.launch_cmd(
        "/usr/sbin/setextattr user nels.project.name \"%s\" \"%s\" " % (cleanup_project_name(name), project_home))


def project_home(pid):
    return path.join(storage_facade.PROJECTS_ROOT_DIR, project_id_to_name(pid))


def project_add(pid, name):
    if project_exists(pid):
        run_utils.exit_fail("Project with same project pid exists")

    project_name = project_id_to_name(pid)
    for role in ("", "-a", "-p"):
        grp = "%s%s" % (project_name, role)
        run_utils.launch_cmd("/usr/sbin/pw groupadd -n %s " % grp)
    phome = project_home(pid)
    run_utils.launch_cmd("/bin/mkdir %s " % phome)
    admin_group = "%s-a" % project_name
    run_utils.launch_cmd("/usr/sbin/chown -R root:%s  %s" % (admin_group, phome))
    set_project_name(phome, cleanup_project_name(name))
    storage_facade.permissions_strict_owner_only(phome)
    run_utils.launch_cmd("/bin/chflags -h nosunlink %s" % phome)

    acls = ['everyone@:------a-R-c--s:------:allow',
            'group@:r-xp--a-R-c--s:fd----:allow',
            'owner@:rwxp-daARWcC-s:fd----:allow',
            'group:%s:rwxp--a-R-cC-s:-d----:allow' % project_name,
            'group:%s:r-x---a-R-cC-s:f-----:allow' % project_name,
            'group:%s:----D---------:-d----:deny' % project_name,
            'group:%s-p:rwxpD-aAR-cC-s:fd----:allow' % project_name,
            'group:%s-a:rwxpD-aARWcCos:fd----:allow' % project_name
            ]
    for acl in acls:
        flag = ('-m' if ('@' in acl) else '-a0')
        run_utils.launch_cmd("/bin/setfacl -h %s %s %s " % (flag, acl, phome))

    print ("project added successfully")


def all_project_names():
    feed_utils.info("getting names of all projects")
    names = []
    for pname in os.listdir(path.join(storage_facade.PROJECTS_ROOT_DIR)):
        readable_name = project_name_by_pname(pname)
        if readable_name != '':
            names.append(readable_name)
    return names


def project_members(pid, role):
    return storage_facade.member_nels_ids(role_to_group(pid, role))


def project_rename(pid, new_name):
    if not project_exists(pid):
        run_utils.exit_fail("Project not found")

    new_name = cleanup_project_name(new_name)
    if new_name in all_project_names():
        run_utils.exit_fail('name already used')

    # remove users from the project
    [admin_users, power_users, member_users] = [project_members(pid, MembershipRoles.ADMIN),
                                                project_members(pid, MembershipRoles.POWERUSER),
                                                project_members(pid, MembershipRoles.MEMBER)]
    for uid in admin_users:
        project_user_remove(pid, uid)
    for uid in power_users:
        feed_utils.info(uid)
    for uid in member_users:
        feed_utils.info(uid)

    # set the new name of project
    set_project_name(project_home(pid), new_name)

    # add users back to the project
    for uid in admin_users:
        project_user_add(pid, uid, MembershipRoles.ADMIN)
    for uid in power_users:
        project_user_add(pid, uid, MembershipRoles.POWERUSER)
    for uid in member_users:
        project_user_add(pid, uid, MembershipRoles.MEMBER)
    print("project renamed successfully")


def project_del(pid):
    if not project_exists(pid):
        run_utils.exit_fail("Project not found")
    for role in ("", "-a", "-p"):
        grp = "%s%s" % (project_id_to_name(pid), role)
        if not storage_facade.group_exists(grp):
            continue
        for nels_id in storage_facade.member_nels_ids(grp):
            project_user_remove(pid, nels_id)
        run_utils.launch_cmd("/usr/sbin/pw groupdel -n %s -q " % grp)
    # caution, this deletes all files of the project
    run_utils.launch_cmd("/bin/rm -rf %s" % project_home(pid))
    print ("project removed successfully")


def is_user_member(pid, uid):
    return uid in storage_facade.member_nels_ids(
        role_to_group(pid, MembershipRoles.MEMBER)) or uid in storage_facade.member_nels_ids(
        role_to_group(pid, MembershipRoles.POWERUSER)) or uid in storage_facade.member_nels_ids(
        role_to_group(pid, MembershipRoles.ADMIN))


def project_name_by_id(pid):
    return project_name_by_pname(project_id_to_name(pid))


def project_name_by_pname(pname):
    result = run_utils.launch_cmd(
        "/usr/sbin/getextattr   -q -h user 'nels.project.name'  %s" % path.join(storage_facade.PROJECTS_ROOT_DIR,
                                                                                pname))
    return result[1][0].replace("\n", "")


def project_link_path(pid, uid):
    return path.join(storage_facade.USERS_ROOT_DIR, user_facade.nels_id_to_username(uid), "Projects",
                     project_name_by_id(pid))


def project_user_remove(pid, uid):
    if not is_user_member(pid, uid):
        run_utils.exit_fail("User is not a member of the project")
    proot = path.join(storage_facade.USERS_ROOT_DIR, user_facade.nels_id_to_username(uid), "Projects")
    plink = project_link_path(pid, uid)
    for role in ["member", "poweruser", "admin"]:
        run_utils.launch_cmd(
            "/usr/sbin/pw  groupmod -n %s -d %s " % (role_to_group(pid, role), user_facade.nels_id_to_username(uid)))
    if path.exists(plink):
        run_utils.launch_cmd("/bin/chflags -h nosimmutable,nosunlink %s" % proot)
        run_utils.launch_cmd("/bin/rm -f %s " % plink)
        run_utils.launch_cmd("/bin/chflags -h simmutable,sunlink %s" % proot)


def project_user_add(pid, uid, role):
    if is_user_member(pid, uid):
        project_user_remove(pid, uid)
    proot = path.join(storage_facade.USERS_ROOT_DIR, user_facade.nels_id_to_username(uid), "Projects")
    plink = project_link_path(pid, uid)

    run_utils.launch_cmd(
        "/usr/sbin/pw  groupmod -n %s -m %s" % (role_to_group(pid, role), user_facade.nels_id_to_username(uid)))
    run_utils.launch_cmd("/bin/chflags -h nosimmutable,nosunlink %s" % proot)
    run_utils.launch_cmd(
        "/bin/ln -s -f -h %s %s " % (path.join(storage_facade.PROJECTS_ROOT_DIR, project_id_to_name(pid)), plink))
    run_utils.launch_cmd("/bin/chflags -h simmutable,sunlink %s" % proot)
