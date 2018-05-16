# Introduction
This package is aimed at doing use case tests against the different pieces of NeLS. Based on the use case, a test might touch upon one or more pieces.

# Basic Setup
1. clone the repository:<br/>
git clone . . .
2. navigate to the source dir:<br/>
cd nels.test.integration
3. make sure you have pip installed 
4. install virtualenv: <br/>
pip install virtualenv
5. create virtual environment for python :<br/> 
virtualenv .venv
6. source the virtual environment:<br/> 
source .venv/bin/activate
7. install dependencies:<br/> 
pip install -r requirements.txt
. set proper configuration values in config.py
7. check configurations:<br/>
python config.py

# Users management use cases 
1. get list of nels ids and show one random user: python test_users_list.py
2. search users: python test_users_search.py [-v] [-i nels_id] [-n name] [-e email]  : at least one of the filters should be provided
3. user registration: python test_user_reg.py [-v] e-mail
4. user deletion: python test_user_del.py [-v] nels_id


# Project management use cases
1. get list of project ids and show one random project: python test_projects_list.py
2. search projects: python test_projects_search.py [-v] [-i project_id] [-n name] : at least one of the filters should be provided
3. project creation: python test_project_add.py [-v] name
4. project deletion: python test_project_del.py [-v] project_id

# SSH use cases
1. python test_ssh_usecase.py [-v] nels_id

# Job management use cases
1. python test_copy_usecase.py [-v] nels_id
2. python test_move_usecase.py [-v] nels_id

# Test flows
1. user flow: python test_user_flow.py [-v] email  {register user, test ssh, test copy, test move, delete user}
