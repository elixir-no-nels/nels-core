# -*- encoding: UTF-8 -*-

from setuptools import setup, find_packages

version = '2.1'

setup(name='nels.storage',
      version=version,
      description="A restful NeLS storage service",
      author=["Kidane M. Tekle"],
      author_email=["kidane.tekle@cbu.uib.no"],
      package_dir={'': 'src'},
      packages=find_packages('src'),
      namespace_packages=['nels'],
      include_package_data=True,
      zip_safe=False,
      install_requires=['setuptools','requests==2.9.1','flask==0.10.1','flask-httpauth==2.7.0'],
      entry_points="""
          [console_scripts]
          storage_service = nels.storage.app:main
          test_all = nels.storage_test.all_tests:main
      """,
      )