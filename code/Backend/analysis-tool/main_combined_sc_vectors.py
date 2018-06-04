import input_parser
import pandas as pd
import features as ft
import app_classifier
import config
import timing
import matplotlib.pyplot as plt


def main():
    timing.start_measurement()

    print("Do combined classification using all input files")
    file_contents, labels = input_parser.parse_input_files(config.get_record_dir(), combine_sc_vectors=True)
    X = ft.extract_preconfigured_features(file_contents)
    Y = pd.Series(labels)
    _, total_first_acc, total_second_acc, total_third_acc, total_single_accuracies = app_classifier.do_kfold_cross_validation(X, Y)

    total_acc = [total_first_acc, total_second_acc, total_third_acc]
    plt.plot(total_acc)
    plt.show()

    plt.plot(total_single_accuracies)
    plt.show()

    timing.stop_measurement()


main()
