'''
Created on Feb 27, 2017

@author: kidane
'''
import random
from optparse import OptionParser

import config
from utils import feed_utils
from api import users

def test_user_display(nels_id):
    feed_utils.heading("Trying user display. nels_id: %s" %nels_id)
    user = users.get_user(nels_id)
    if not user:
        feed_utils.failed("failed getting user details")
    else:
        feed_utils.ok("user details: %s" % user )

def test_users_list():
    feed_utils.heading("Trying users list")
    nels_ids =  users.get_nels_ids()
    if not nels_ids:
        feed_utils.failed("get nels ids")
        return False
    feed_utils.ok("fetched %s nels ids" % len(nels_ids))
    
    random_nels_id = nels_ids[random.randrange(0, len(nels_ids)-1)]
    test_user_display(random_nels_id)
    return True

if __name__ == "__main__":
    parser = OptionParser(usage='usage: %prog nels_id')
    parser.add_option('-v', '--verbose', dest="verbose", action="store_true", help='turn verbosity on', default=False)
    (options, args) = parser.parse_args()
    feed_utils.VERBOSE = options.verbose
    test_users_list()
    