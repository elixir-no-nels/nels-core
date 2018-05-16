Introduction
===
This package is an oauth2 server build using python-oauth2 lib and tornado packages.<br/>
It receives authentication information from the NeLS portal and issues oauth2 tokens as well as provide introspection for authorized services. 

Basic setup
===
```
cd nels.oauth2
virtualenv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

Configuration
===
Edit the file `config.json` and provide proper values

Running the service
===
```
cd nels.oauth2
source .venv/bin/activate
python oauth2_server.py
```