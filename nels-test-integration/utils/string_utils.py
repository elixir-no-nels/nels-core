'''
Created on Feb 18, 2017

@author: kidane
'''

import hashlib

def right_of_last_occurance(source, toFind):
    ret = ""
    if toFind in source:
        splitText = source.split(toFind)
        if(len(splitText)>0):
            ret = splitText[len(splitText)-1]
    return ret

def right_of_first_occurance(source, toFind):
    ret = ""
    if toFind in source:
        splitText = source.split(toFind,1)
        if(len(splitText)>0):
            ret = splitText[len(splitText)-1]
    return ret

def left_of_first_occurance(source, toFind):
    ret = ""
    if toFind in source:
        splitText = source.partition(toFind)
        ret = splitText[0]
    return ret

def left_of_last_occurance(source, toFind):
    ret = ""
    if toFind in source:
        splitText = source.rpartition(toFind)
        ret = splitText[0]
    return ret

def is_number(text):
    try:
        long(text)
    except:
        return False
    return True

def append_with_delimiter(original,new,separator,before='',after=''):
    if original ==None:
        original = ""
    if new == None:
        new = ""
    ret = original
    if original!='' and new!='':
        ret = ret +separator
    ret = ret + new
    if before !='':
        ret = before+ret
    if after !='':
        ret = ret + after 
    return ret

def right_of_index(text,index):
    ret = ""
    if index>=0 and index<len(str):
        ret = text[index+1:]
    return ret

def left_of_index(text,index):
    if index>=0:
        return text[:index]
    return ""

def md5(text):
    return hashlib.md5(text).hexdigest()