'''
Created on Jan 20, 2016

@author: Kidane
'''


import unittest

from nels.storage.config import storage_config  

class configTest (unittest.TestCase):
    
    def test_config(self):
        self.assertNotEqual(storage_config.PORT, 0) 

if __name__ == "__main__":
    unittest.main()