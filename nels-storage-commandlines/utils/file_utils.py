import os
from os import path
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


def get_tree_size(folder):
    """Return total size of files in given path and subdirs."""
    total = 0
    for child in os.listdir(folder):
        child_pth = path.join(folder,child)
        if path.islink(child_pth):
            continue
        if path.isdir(child_pth):
            total += get_tree_size(child_pth)
        else:
            total += path.getsize(child_pth)
    return total
