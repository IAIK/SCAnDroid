# The following code is based on the ProcHarvester implementation
# See https://github.com/IAIK/ProcHarvester/tree/master/code/analysis%20tool

from scipy.spatial.distance import euclidean
from fastdtw import fastdtw
import numpy as np
from cdtw import pydtw


def dtw(rec1, rec2, print_info=False):
    return fast_c_dtw_implementation(rec1, rec2, print_info)
    # return dtw_considering_time_steps(rec1, rec2)


def jaccard_distance(r1, r2):
    # Apply differential first to avoid comparing absolute values
    diff_s1 = np.diff(r1.np_values)
    diff_s2 = np.diff(r2.np_values)
    intersection_cardinality = len(set.intersection(*[set(diff_s1), set(diff_s2)]))
    union_cardinality = len(set.union(*[set(diff_s1), set(diff_s2)]))

    if union_cardinality > 0 and intersection_cardinality > 0:
        res = intersection_cardinality / float(union_cardinality)
    else:
        return 99999.0  # no match

    res = 1.0 / res  # use reciprocal values since out knn classifier minimizes distances
    return res


def fast_c_dtw_implementation(rec1, rec2, print_info=False):
    s1 = rec1.np_values
    s2 = rec2.np_values
    if print_info:
        print((s1))
        print((rec1.values))

    """
    Step class

        Class containts different step patterns for dynamic time warping algorithm.
        There are folowing step patterns available at the moment:
        Usual step patterns:
            'dp1' 
            'dp2' 
            'dp3'
        Sakoe-Chiba classification:
            'p0sym':
            'p0asym':
            'p05sym':
            'p05asym':
            'p1sym':
            'p1asym':
            'p2sym':
            'p2asym':

        You can see step pattern definition using print_step method
    """
    d = pydtw.dtw(s1, s2, pydtw.Settings(dist='manhattan',
                                         step='p2sym',  # Sakoe-Chiba symmetric step with slope constraint p =
                                         window='palival',  # type of the window: scband, itakura, palival, itakura_mod
                                         param=7.0,  # window parameter
                                         norm=True,  # normalization
                                         compute_path=print_info))

    distance = d.get_dist()
    if not np.isfinite(distance):
        distance = 9999999.
    if print_info:
        print(distance)
        d.plot_alignment()
        #d.plot_mat_path()
        #d.plot_seq_mat_path()
        print(d.get_path())
        print("next")

    if print_info:
        return float(distance), d.get_path()
    return float(distance)


step_cnt = 0


def dtw_considering_time_steps(rec1, rec2):
    s1 = rec1.np_values
    s2 = rec2.np_values

    global step_cnt
    step_cnt += 1
    if step_cnt % 150 == 0:
        print(str(step_cnt) + " dtw computations finished")
    distance, _ = fastdtw(s1, s2, radius=1, dist=euclidean)
    return distance
