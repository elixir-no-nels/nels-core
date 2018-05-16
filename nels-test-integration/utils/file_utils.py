'''
Created on Feb 18, 2017

@author: kidane
'''
import os
import sys

def write_to_file(file_path,file_content):
    filehandle = open(file_path,'w')
    filehandle.write(file_content)
    filehandle.close()        

def read_file_content(file_path):
    try:
        file_handle = open(file_path, 'r')
    except IOError:
        return None
    content = file_handle.read() 
    return content

def is_file(file_path):
    return os.path.isfile (file_path)


def get_script_dir():
    pathname = os.path.dirname(sys.argv[0])       
    return os.path.abspath(pathname)

