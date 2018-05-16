'''
Created on Jan 20, 2016

@author: Kidane
'''

import unittest

from nels.storage_test.config_test import configTest


def main():
    suite = unittest.TestSuite()
    suite.addTest(unittest.makeSuite(configTest))

    unittest.TextTestRunner(verbosity=2).run(suite)

if __name__ == "__main__":
    main()