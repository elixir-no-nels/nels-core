Introduction
===
This package computes the disk usage of NeLS users and NeLS projects and feeds the numbers to the NeLS public api services by making a RESTful invocation. 

Basic setup
===
```
cd nels.storage.statistics
virtualenv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

Running of commands
===
```
cd nels.storage.statistics
source .venv/bin/activate
python some-command.py some-options some-arguments
```

Supported functions
===
1. python project_disk.py 
2. python user_disk.py
 