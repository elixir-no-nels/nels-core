'''
Created on Jan 20, 2016

@author: Kidane
'''

import ConfigParser
from os import path

class config(object):

    def __init__(self):
        self.PORT = 0
        self.HOST = ''
        self.BASE_SERVICE_URL = ''
        self.USERNAME  = ''
        self.PASSWORD = ''
        self.Authorized_Clients ={}
        
        #apply configs
        self.apply_config(self.read_config())
   
    def read_config(self):
        conf_file=path.join('parts', 'etc', 'nels.conf')
        if not path.isfile(conf_file):
            raise("Configuration file missing")
        parser = ConfigParser.RawConfigParser()
        parser.read(conf_file)
        return parser
        
    def apply_config(self,parser):
        self.PORT = int(parser.get("Parameters","PORT"))
        self.HOST = str(parser.get("Parameters","HOST"))
        self.BASE_SERVICE_URL = str(parser.get("Parameters","BASE_SERVICE_URL"))
        self.USERNAME = str(parser.get("Parameters","USERNAME"))
        self.PASSWORD = str(parser.get("Parameters","PASSWORD"))
        
        clients = parser.items('Authorized_Clients')
        for client in clients:
            self.Authorized_Clients[client[0]] = client[1]
        
storage_config = config()