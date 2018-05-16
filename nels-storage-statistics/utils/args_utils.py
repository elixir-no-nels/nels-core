import sys

import string_utils

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

def require_arg_in_list(parser, args, index, arg_name, allowed_list):
    if args[index] not in allowed_list:
        exit_parser(parser, "%s must be one of the following values :%s" % (arg_name,allowed_list))
        
