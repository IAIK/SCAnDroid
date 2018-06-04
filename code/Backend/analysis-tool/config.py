# The following code is based on the ProcHarvester implementation
# See https://github.com/IAIK/ProcHarvester/tree/master/code/analysis%20tool

import preprocessing

RECORD_DIR = 'websites_results/Android_8.1/'

DTW_COMPARISON_DIR = 'app_start_results/dtw_comparison/'

PREPROCESS_FUNCTION = preprocessing.custom_left_neighbour_interpolate
SKIP_FIRST_MILLISECONDS = 0
NORMALIZE = True

PRINT_NR_OF_MEASUREMENTS_PER_FIRST_SECOND = False

# Use this to check whether excluding certain side channels improves the results
EXCLUDE_SIDECHANNELS = False
EXCLUDED_SIDECHANNELS = [
]

# Warning: The file order of the side channels is relevant for the knn implementation,
# since it is used as criterion for even majority vote situations
USE_TARGETED_SIDECHANNELS = False  # Use all files in target directory when this option is disabled
TARGETED_SIDECHANNELS = [
]

MIN_EVENT_NUMBER = 30

FOLDS = 8


def get_record_dir():
    return RECORD_DIR


K_NEAREST_CNT = 2
FILE_ENDINGS = '*.txt'
RECORD_BASE_DIR = 'record_files/'
RESULTS_DIR = "results/"
