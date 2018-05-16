
from os import path,listdir
from storage_facade import feed_utils,run_utils

from facades import storage_facade

def nels_id_to_username(nels_id):
    return ("u%s" % storage_facade.nels_id_to_sys_id(nels_id))

def user_exists(nels_id):
    feed_utils.info("checking if user exists : nels_id =%s " % nels_id)
    return run_utils.launch_cmd("/usr/bin/env id %s" % nels_id_to_username(nels_id))[0] == 0

def user_home(nels_id):
    return path.join(storage_facade.USERS_ROOT_DIR,nels_id_to_username(nels_id))

def user_add(nels_id):
    feed_utils.heading("add user: nels_id=%s" % nels_id)
    if user_exists(nels_id):
        run_utils.exit_fail("user already exists")

    username = nels_id_to_username(nels_id)
    userhome = user_home(nels_id)
    if path.exists(userhome):
        run_utils.exit_fail("user's home folder already exists. check %s " % userhome)

    nels_users_group = 'nels_users'
    shell = 'sh'
    run_utils.launch_cmd('/usr/sbin/pw useradd -n %s -c %s -g %s -s %s -m -b %s +1y -h -' % (username, username, nels_users_group, shell, storage_facade.USERS_ROOT_DIR))
    storage_facade.permissions_strict_owner_only(userhome)
    run_utils.launch_cmd('/bin/setfacl -h -a2 %s %s' % ('g:nels_storage_admin:xaRcs:fd:allow', userhome))

    for fldr in ['Personal', 'Projects']:
        pth = path.join(userhome,fldr)
        run_utils.launch_cmd("/bin/mkdir -p %s" % pth, username)
        storage_facade.permissions_add_full_control_to_groupname(pth, "nels_storage_admin")

    #create ssh keys
    ssh_dir = path.join(userhome,".ssh")
    run_utils.launch_cmd("/bin/mkdir -p %s" % ssh_dir, username)
    storage_facade.permissions_strict_owner_only(ssh_dir)
    run_utils.launch_cmd("/usr/bin/ssh-keygen -b 4096 -f %s -q -N \"\"" % path.join(ssh_dir,"nels"), username)
    run_utils.launch_cmd("/bin/cp %s %s " %(path.join(ssh_dir,"nels.pub"),path.join(ssh_dir,"authorized_keys")), username)
    print ("user added successfully")

def user_del(nels_id):
    feed_utils.heading("deleting user: nels_id=%s" % nels_id)
    if not user_exists(nels_id):
        run_utils.exit_fail("user not found")
    username = nels_id_to_username(nels_id)
    userhome = path.join(storage_facade.USERS_ROOT_DIR, username)
    projects_dir = path.join(userhome,"Projects")
    if  path.exists(projects_dir):
        run_utils.launch_cmd('/bin/chflags -h nosimmutable,nosunlink %s ' % projects_dir)
        for prj in listdir(projects_dir):
            pth = path.join(projects_dir,prj)
            run_utils.launch_cmd('/bin/chflags -h nosimmutable,nosunlink %s ' % pth)

    result = run_utils.launch_cmd('/usr/sbin/pw userdel -r -n %s' % username)
    print ("user deleted successfully") if result[0] == 0 else run_utils.exit_fail("user not deleted. try running ")
