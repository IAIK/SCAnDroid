# The following code is based on the ProcHarvester implementation
# See https://github.com/IAIK/ProcHarvester/tree/master/code/analysis%20tool

import numpy as np


def normalize(np_values, _=None):
    mean = np.mean(np_values)
    normalized = np_values - int(mean)
    return normalized


def subtract_min(np_values, _=None):
    minimum = np.min(np_values)
    result = np_values - int(minimum)
    return result


def none(np_values, _=None):
    return np_values


def subtract_min_and_normalize(np_values, _=None):
    return normalize(subtract_min(np_values))


def normalize_and_subtract_min(np_values, _=None):
    return subtract_min(normalize(np_values))


def diff(np_array, _=None):
    diff = np.diff(np_array).astype(int)
    if len(diff) == 0:
        return [0]
    return diff


def standardize(np_values, _=None):
    stddev = np.std(np_values)
    return np_values / stddev


def std_interpolate(np_values, np_time_stamps):
    return standardize(custom_left_neighbour_interpolate(np_values, np_time_stamps))


# This is a modified left neighbour interpolation that guarantees that each value in the
# input time series is also present in the interpolation at least once, regardless of time stamps
def custom_left_neighbour_interpolate(np_values, np_time_stamps):
    last_time_stamp = np_time_stamps[len(np_time_stamps) - 1]
    x_steps = np.linspace(np_time_stamps[0], last_time_stamp, 45, endpoint=True)  # create equally spaced timesteps
    #print("\n\nBefore interpolating, values: ", np_values, "\ntime stamps: ", np_time_stamps, "\nx_steps:", x_steps)
    #print("np_values.shape", np_values.shape)
    interpolation = left_nearest_modified(np_values, np_time_stamps, x_steps)
    #print("np.array(interpolation).shape", np.array(interpolation).shape)
    #print("\nAfter interpolating: ", interpolation)

    assert (interpolation[0] == np_values[0])
    assert (interpolation[len(interpolation) - 1] == np_values[len(np_values) - 1])
    return np.array(interpolation)


def standard_interpolate(np_values, np_time_stamps, x_steps):
    return np.interp(x_steps, np_time_stamps, np_values, period=None)


def right_nearest_modified(np_values, np_time_stamps, x_steps):
    last_time_stamp_index = len(np_time_stamps) - 1
    interpolation = []
    pos = 0
    for time_stamp in x_steps:
        new_time_stamp = False
        while pos < last_time_stamp_index and time_stamp > np_time_stamps[pos]:
            pos += 1
            interpolation.append(np_values[pos])
            new_time_stamp = True
        if not new_time_stamp:  # add value at least once
            interpolation.append(np_values[pos])

    # interpolation.append(np_values[last_time_stamp_index])
    return interpolation


def left_nearest_modified(np_values, np_time_stamps, x_steps):
    interpolation = []
    pos = 0
    step_cnt = 0
    for time_stamp in np_time_stamps:
        interpolation.append(np_values[pos])
        while step_cnt < len(x_steps) - 1 and time_stamp > x_steps[step_cnt]:
            interpolation.append(np_values[pos])
            step_cnt += 1
        pos += 1
    return interpolation
