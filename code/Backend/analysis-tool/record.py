# The following code is based on the ProcHarvester implementation
# See https://github.com/IAIK/ProcHarvester/tree/master/code/analysis%20tool

import numpy as np
import preprocessing
import config

class Record:

    def __init__(self):
        self.values = []
        self.time_stamps = []

    def create_numpy_arrays(self, preprocess_function):
        # always normalize before doing any other preprocessing
        if config.NORMALIZE:
            self.np_values = preprocessing.normalize(np.array(self.values))
        else:
            self.np_values = preprocessing.none(np.array(self.values))
        self.np_time_stamps = np.array(self.time_stamps)
        #print("Before pre processing:", self.np_values)
        if preprocess_function:
            self.np_values = preprocess_function(self.np_values, self.np_time_stamps)
        #print("After pre processing:", self.np_values)


class FileContent:
    def __init__(self, file_name):
        self.records = []
        self.file_name = file_name

    def do_preprocessing(self, preprocess_function=None):
        # print("Preprocessing numpy arrays for file " + self.file_name)
        for rec in self.records:
            rec.create_numpy_arrays(preprocess_function)