'''
Created on Mar 30, 2017

@author: kidane
'''

import string_utils

def scope_array_to_list(scope_array):
    combined_scope = ''
    for scope in scope_array:
        combined_scope = string_utils.append_with_delimiter(combined_scope, scope, " ")
    return combined_scope