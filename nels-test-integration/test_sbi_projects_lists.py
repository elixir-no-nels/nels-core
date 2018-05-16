__author__ = 'Siri Kallhovd'
__email__ = 'siri.kallhovd@.uib.no'
__date__ = '30/Jan/2018'

import random
from optparse import OptionParser

import config
from utils import feed_utils
from api import sbi_project
from test_sbi_project_list_users import test_project_list_all_users


def test_project_display(project_id):
    feed_utils.heading("Trying project display. project_id: %s" % project_id)
    project_info = sbi_project.get_project(project_id)
    if not project_info:
        feed_utils.failed("failed getting project details")
    else:
        feed_utils.ok("project details: %s" % project_info)


def test_project_display_dataset(project_id, dataset_id):
    feed_utils.heading("Trying dataset in project display. project_id: %s, dataset_id %s" % (project_id, dataset_id))
    project_info = sbi_project.get_dataset_in_project(project_id, dataset_id)
    if not project_info:
        feed_utils.failed("failed getting dataset details")
    else:
        feed_utils.ok("dataset details: %s" % project_info)


def test_project_display_subtype(project_id, dataset_id, subtype_id):
    feed_utils.heading("Trying subtype of dataset in project display. project_id: %s, dataset_id %s, subtype_id %s" % (
    project_id, dataset_id, subtype_id))
    project_info = sbi_project.get_subtype_in_dataset_in_project(project_id, dataset_id, subtype_id)
    if not project_info:
        feed_utils.failed("failed getting subtype details")
    else:
        feed_utils.ok("subtype details: %s" % project_info)


def test_project_list_all_subtypes(project_id, dataset_id):
    feed_utils.heading(
        "Trying list subtypes of a dataset in project. project_id: %s, dataset_id %s" % (project_id, dataset_id))
    project_info = sbi_project.get_subtypes_in_dataset_in_project(project_id, dataset_id)

    if project_info == None:
        feed_utils.failed("failed getting project dataset subtypes")
    elif project_info == []:
        feed_utils.ok("project without dataset subtypes: %s" % project_info)

    else:
        feed_utils.ok("project dataset subtypes: %s" % project_info)

        # random_subtype_id = project_info[random.randrange(0, len(project_info) )]

        # test_project_display_subtype(project_id,dataset_id, random_subtype_id)


def test_project_list_all_datasets(project_id):
    feed_utils.heading("Trying list datasets in project. project_id: %s" % project_id)
    project_info = sbi_project.get_datasets_in_project(project_id)

    if project_info == None:
        feed_utils.failed("failed getting project datasets")
    elif project_info == []:
        feed_utils.ok("project without datasets: %s" % project_info)

    else:
        feed_utils.ok("project datasets: %s" % project_info)

        # random_dataset_id = project_info[random.randrange(0, len(project_info) )]
        # test_project_display_dataset(project_id, random_dataset_id)
        # test_project_list_all_subtypes(project_id, random_dataset_id)


def test_projects_list():
    feed_utils.heading("Trying projects list")
    project_ids = sbi_project.get_project_ids()
    if not project_ids:
        feed_utils.failed("get project ids")
        return False
    feed_utils.ok("fetched %s project ids" % len(project_ids))

    random_project_id = project_ids[random.randrange(0, len(project_ids) - 1)]
    test_project_display(random_project_id)
    test_project_list_all_datasets(random_project_id)
    test_project_list_all_users(random_project_id)


if __name__ == "__main__":
    parser = OptionParser(usage='usage: %prog project_id')
    parser.add_option('-v', '--verbose', dest="verbose", action="store_true", help='turn verbosity on', default=False)
    (options, args) = parser.parse_args()
    feed_utils.VERBOSE = options.verbose

    test_projects_list()

    feed_utils.heading("")
    project_id = 1125311
    dataset_id = 1124859
    subtype_id = 1124887

    test_project_display(project_id)
    test_project_list_all_users(project_id)
    test_project_list_all_datasets(project_id)

    json_dataset_type_array = sbi_project.get_dataset_types()
    feed_utils.info(json_dataset_type_array)
    feed_utils.heading("")

    test_project_display_dataset(project_id, dataset_id)
    test_project_list_all_subtypes(project_id, dataset_id)

    feed_utils.heading("")
    test_project_display_subtype(project_id, dataset_id, subtype_id)
