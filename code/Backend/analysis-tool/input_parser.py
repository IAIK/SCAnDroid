# The following code is based on the ProcHarvester implementation
# See https://github.com/IAIK/ProcHarvester/tree/master/code/analysis%20tool

# Parse input files recorded by the logging app
import os, glob
from record import Record, FileContent
import config


def is_excluded(file):
    if not config.EXCLUDE_SIDECHANNELS:
        return False
    file_name = os.path.basename(file)
    for exclude_entry in config.EXCLUDED_SIDECHANNELS:
        if exclude_entry in file_name:
            print("\nExclude " + file_name + " - found in exclude list")
            return True
    return False


def parse_input_files(input_dir, combine_sc_vectors, ignore_min_event_number=False):
    
    print('\nParse input files in ' + input_dir)

    # Top level cells (containing all files)
    file_contents = []
    first_file = True
    output_labels = []
    for file in glob.glob(os.path.join(config.RECORD_BASE_DIR + input_dir, config.FILE_ENDINGS)):

        if is_excluded(file):
            continue

        print("file: " + file)

        file_content, labels = parse_file(file)

        if not ignore_min_event_number and len(file_content.records) < config.MIN_EVENT_NUMBER:
            print("\nToo few records: Skip file " + file_content.file_name)
            continue

        file_contents.append(file_content)

        if not combine_sc_vectors:
            output_labels.append(labels)
        else:
            if first_file:
                output_labels = labels
            else:
                if output_labels != labels:
                    error_message = ('label name line mismatch - input files do not fit together - '
                                     'enable EXPLORATION_MODE to analyze each input file separately')
                    raise ValueError(error_message)

        first_file = False

    return file_contents, output_labels


def parse_file(file):

    # Mid level cells(containing all the content of a file)
    labels = []
    fileContent = FileContent(os.path.basename(file))
    line_cnt = 0

    with open(file) as f:
        for line in f:

            if line.startswith("||"):
                print("removed first line: " + line)
                continue

            # Low level cells(contain recorded traces)
            line_token = line.split(',')

            rec = Record()

            number_of_data_points = len(line_token) - 2  # skip the label name at the last position
            nr_of_measurements = 0
            for entry_cnt in range(0, number_of_data_points):
                entry = line_token[entry_cnt].split('|')
                value = float(entry[0])  # actual value
                time_stamp = int(entry[1])  # relative time stamp

                if time_stamp <= 1000:
                    nr_of_measurements += 1
                elif nr_of_measurements != 0:
                    if config.PRINT_NR_OF_MEASUREMENTS_PER_FIRST_SECOND:
                        print("nr of measurements per second: ", nr_of_measurements)
                    nr_of_measurements = 0

                if time_stamp >= config.SKIP_FIRST_MILLISECONDS:
                    rec.values.append(value)
                    rec.time_stamps.append(time_stamp)

            current_label = line_token[number_of_data_points].strip()
            labels.append(current_label)

            fileContent.records.append(rec)
            line_cnt += 1

    fileContent.do_preprocessing(preprocess_function=config.PREPROCESS_FUNCTION)

    return fileContent, labels
