# The following code is based on the ProcHarvester implementation
# See https://github.com/IAIK/ProcHarvester/tree/master/code/analysis%20tool

import input_parser
import pandas as pd
import app_classifier
import config
import timing
import matplotlib.pyplot as plt
import glob
import os


class ClassificationResult:
    def __init__(self, accuracy, file_name):
        self.accuracy = accuracy
        self.file_name = file_name

    def __str__(self):
        return "Total accuracy: " + str(self.accuracy) + " for " + self.file_name


def round_float(number):
    return "{0:.5f}".format(number)


def explorative_classification():
    file_contents, label_list = input_parser.parse_input_files(config.get_record_dir(), combine_sc_vectors=False)
    results = []
    results_first = []
    results_second = []
    results_third = []
    single_results = []

    # print("file content")
    # for idx, fc in enumerate(file_contents):
    #    print(str(idx) + " " + str(fc.file_name))

    # print("labellist")
    # print (label_list)

    for idx, fc in enumerate(file_contents):
        labels = label_list[idx]

        print("\nEvaluate ", fc.file_name)
        # print("labels")
        # print(labels)

        X = [fc]
        Y = pd.Series(labels)
        # print("Y")
        # print(Y)

        total_accuracy, total_first_acc, total_second_acc, total_third_acc, total_single_accuracies = app_classifier.do_kfold_cross_validation(
            X, Y, verbose=True, file_name=fc.file_name[:-4])
        results.append(ClassificationResult(round_float(total_accuracy), fc.file_name))
        results_first.append(ClassificationResult(round_float(total_first_acc), fc.file_name))
        results_second.append(ClassificationResult(round_float(total_second_acc), fc.file_name))
        results_third.append(ClassificationResult(round_float(total_third_acc), fc.file_name))
        single_results.append([])
        for total_single_accuracy in total_single_accuracies:
            single_results[idx].append(ClassificationResult(round_float(total_single_accuracy), fc.file_name))

    results.sort(key=lambda classification_result: classification_result.accuracy, reverse=True)
    results_first.sort(key=lambda classificationResult: classificationResult.accuracy, reverse=True)
    results_second.sort(key=lambda classificationResult: classificationResult.accuracy, reverse=True)
    results_third.sort(key=lambda classificationResult: classificationResult.accuracy, reverse=True)
    # for single_result in single_results:
    #   single_result.sort(key=lambda classification_result: classification_result.accuracy, reverse=True)

    print("\nSummary for files in " + config.get_record_dir() + ":\n")
    for r in results:
        print(r)
    print("\nSummary of first for files in " + config.get_record_dir() + ":\n")
    for r in results_first:
        print(r)
    print("\nSummary of second for files in " + config.get_record_dir() + ":\n")
    for r in results_second:
        print(r)
    print("\nSummary of third for files in " + config.get_record_dir() + ":\n")
    for r in results_third:
        print(r)

    for single_result in zip(single_results):
        if not os.path.exists(config.RECORD_BASE_DIR + config.get_record_dir() + config.RESULTS_DIR):
            os.makedirs(config.RECORD_BASE_DIR + config.get_record_dir() + config.RESULTS_DIR)
        # print("\nSummary of for files in " + config.get_record_dir() + ":\n")
        for r_1 in single_result:
            file = open(config.RECORD_BASE_DIR + config.get_record_dir() + config.RESULTS_DIR + r_1[0].file_name, "w")
            for idx, r in enumerate(r_1):
                file.write(str(idx + 1) + ", " + str(r.accuracy) + "\n")
            file.close()

    # for result1, result2, result3 in zip(results_first, results_second, results_third):
    #    #result = [result1.accuracy, result2.accuracy, result3.accuracy]
    #    #plt.plot(result, label=result1.file_name)
    #    if not os.path.exists(config.RECORD_BASE_DIR + config.get_record_dir() + config.RESULTS_DIR):
    #        os.makedirs(config.RECORD_BASE_DIR + config.get_record_dir() + config.RESULTS_DIR)
    #    file = open(config.RECORD_BASE_DIR + config.get_record_dir() + config.RESULTS_DIR + result1.file_name, "w")
    #    file.write("Pos, Accuracy\n")
    #    for idx, result_accuracy in enumerate(single_result):
    #        file.write(str(idx + 1) + ", " + str(result_accuracy) + "\n")
    #    file.close()

    # plt.legend()
    # plt.show()


def main():
    timing.start_measurement()
    print("Do explorative classification with separate input files")
    explorative_classification()
    timing.stop_measurement()


main()
