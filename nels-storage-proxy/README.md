Introduction
===
This package is a proxy http service to expose limited features of the NeLS storage service 

Basic setup
===
```
cd nels-storage-proxy
virtualenv .venv
source .venv/bin/activate
python bootstrap.py
./bin/buildout
```

Configuration
===
Edit the file `etc/nels.conf.in` and provide proper values

Running the service
===
```
cd nels-storage-proxy
./bin/storage_service
```