Introduction
===
This package provides command lines that make it easy to manage the NeLS storage file system.<br/>
The NeLS storage currently is designed for FreeBSD with ZFS file system for supporting advanced ACLs

Basic setup
===
* cd nels.storage.commandlines
* virtualenv .venv
* source .venv/bin/activate
* pip install -r requirements.txt

Running of commands
===
* cd nels.storage.commandlines
* source .venv/bin/activate
* python some-command.py some-options some-arguments

Supported functions
===
1. python nelsid_to_username.py  nels-id
2. python user_info.py [-v] nels-id
3. python user_add.py [-v] nels-id
4. python user_del.py [-v] nels-id (caution: deletes the user's home folder)
5. python project_info.py  [-v] project-id
6. python project_add.py  [-v] project-id name
7. python project_del.py  [-v] project-id (caution: deletes the project home folder)
8. python project_member_add [-v]  project-id nels_id role {member, poweruser, admin}
9. python project_member_remove [-v] project-id nels_id 