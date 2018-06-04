# The following code is based on the ProcHarvester implementation
# See https://github.com/IAIK/ProcHarvester/tree/master/code/analysis%20tool

from sklearn.model_selection import StratifiedKFold
from sklearn.metrics import accuracy_score
from sklearn.metrics import classification_report
import plot_confusion_matrix
import numpy as np
import pandas as pd
import precomputed_knn_selector
import config
import os


def do_classification(X, Y, train_indices, test_indices, dist_matrices=None):
    Y_train, Y_test = Y[train_indices], Y[test_indices]

    if dist_matrices is None:
        dist_matrices = precomputed_knn_selector.init_dist_matrices(X)
    predictions, first_pred, second_pred, third_pred, single_predictions = precomputed_knn_selector.predict_based_on_distance_matrix(
        X, Y_train, test_indices, train_indices, dist_matrices)

    accuracy = accuracy_score(Y_test, predictions)
    acc_first = accuracy_score(Y_test, first_pred)
    acc_second = accuracy_score(Y_test, second_pred) + acc_first
    acc_third = accuracy_score(Y_test, third_pred) + acc_second

    single_accuracies = []
    total_accuracy = 0
    for single_prediction in single_predictions:
        single_accuracy = accuracy_score(Y_test, single_prediction)
        total_accuracy += single_accuracy
        single_accuracies.append(total_accuracy)

    return Y_test, predictions, accuracy, acc_first, acc_second, acc_third, single_accuracies


def do_kfold_cross_validation(X, Y, verbose=True, file_name=""):
    folds = config.FOLDS
    printv("\nSelecting rows for " + str(folds) + "-fold validation", verbose)
    kf = StratifiedKFold(n_splits=folds, shuffle=True)
    kf.get_n_splits()

    # Initialize classification performance measures
    unique_labels = Y.unique()
    cnf_mat = pd.DataFrame(np.zeros((len(unique_labels), len(unique_labels))), columns=unique_labels)
    # print("-------------------------- cnf_mat --------------------------")
    # print(cnf_mat)
    cnf_mat.set_index(keys=unique_labels, inplace=True)
    # print("-------------------------- cnf_mat 2 --------------------------")
    # print(cnf_mat)
    Y_test_all_folds = []
    predictions_all_folds = []
    summed_accuracy = 0
    summed_first_acc = 0
    summed_second_acc = 0
    summed_third_acc = 0
    summed_single_accuracies = []

    fold_cnt = 1
    firstFileContent = X[0]
    split_var = firstFileContent.records

    dist_matrices = precomputed_knn_selector.init_dist_matrices(X)

    # print("-------------------------- dist_matrices --------------------------")
    # print(dist_matrices)

    for train_indices, test_indices in kf.split(split_var, Y):

        printv("\nFold: " + str(fold_cnt), verbose)

        Y_test, predictions, accuracy, acc_first, acc_second, acc_third, single_accuracies = do_classification(X, Y,
                                                                                                               train_indices,
                                                                                                               test_indices,
                                                                                                               dist_matrices)

        if verbose:
            for idx, pred in enumerate(predictions):
                cnf_mat.ix[Y_test.iloc[idx], pred] += 1

        printv("Accuracy:" + str(accuracy), verbose)
        summed_accuracy += accuracy
        summed_first_acc += acc_first
        summed_second_acc += acc_second
        summed_third_acc += acc_third
        for idx, single_accuracy in enumerate(single_accuracies):
            if idx >= len(summed_single_accuracies):
                summed_single_accuracies.append(single_accuracy)
            else:
                summed_single_accuracies[idx] += single_accuracy
        fold_cnt += 1

        Y_test_all_folds.extend(Y_test.values.tolist())
        predictions_all_folds.extend(predictions.values.tolist())

    total_accuracy = summed_accuracy / folds
    total_first_acc = summed_first_acc / folds
    total_second_acc = summed_second_acc / folds
    total_third_acc = summed_third_acc / folds
    total_single_accuracies = []
    for idx, summed_single_accuracy in enumerate(summed_single_accuracies):
        total_single_accuracies.append(summed_single_accuracy / folds)

    if verbose:
        classification_rep = classification_report(Y_test_all_folds, predictions_all_folds)
        printv(classification_rep, verbose)
        if not os.path.exists(config.RECORD_BASE_DIR + config.get_record_dir() + config.RESULTS_DIR):
            os.makedirs(config.RECORD_BASE_DIR + config.get_record_dir() + config.RESULTS_DIR)
        file = open(config.RECORD_BASE_DIR + config.get_record_dir() + config.RESULTS_DIR + file_name +
                    "_classification_report.txt", "w")
        file.write(classification_rep)
        file.close()

    print("\nTotal accuracy over all folds: " + str(total_accuracy))
    print("Total 1st accuracy over all folds: " + str(total_first_acc))
    print("Total 2nd accuracy over all folds: " + str(total_second_acc))
    print("Total 3rd accuracy over all folds: " + str(total_third_acc))
    print("Total single accuracies over all folds:", total_single_accuracies)

    if verbose:
        # plot_confusion_matrix.show_confusion_matrix(cnf_mat.values.astype(int), unique_labels)
        if not os.path.exists(config.RECORD_BASE_DIR + config.get_record_dir() + config.RESULTS_DIR):
            os.makedirs(config.RECORD_BASE_DIR + config.get_record_dir() + config.RESULTS_DIR)
        # print("\nSummary of for files in " + config.get_record_dir() + ":\n")
        cnf_mat.to_csv(config.RECORD_BASE_DIR + config.get_record_dir() + config.RESULTS_DIR + file_name +
                       "_confusion_matrix.txt", sep=' ')

    return total_accuracy, total_first_acc, total_second_acc, total_third_acc, total_single_accuracies


def printv(str, verbose):
    if verbose:
        print(str)
