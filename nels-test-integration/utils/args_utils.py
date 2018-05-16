'''
Created on Feb 18, 2017

@author: kidane
'''

import sys

from utils import string_utils

def exit_parser(parser, msg):
    print("\nerror: %s\n" % msg)
    parser.print_help()
    sys.exit(1)

def require_args_length(parser, args, length):
    if not (len(args) == length):
        exit_parser(parser, "invalid number of parameters")

def require_arg_number(parser, args, index, arg_name):
    if not string_utils.is_number(args[index]):
        exit_parser(parser, "%s must be a number" % arg_name)
        
