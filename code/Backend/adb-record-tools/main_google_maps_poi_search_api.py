# The following code is based on the ProcHarvester implementation
# See https://github.com/IAIK/ProcHarvester/tree/master/code/adb-record-tools

import argparse
import logging_functions as lf
import randomized_record
import time
import config
from threading import Thread

CHECK_CONFIGURATION = False

POI_SEARCH_QUERIES = [
    "sydney+harbour",
    "empire+state+building",
    "burj+khalifa",
    "petronas+towers",
    "wencelas+square+prague",
    "big+ben",
    "mirabell+gardens",
    "eiffel+tower",
    "cape+town",
    "peking",
    "the+great+wall+of+china",
    "taipei+101",
    "colosseum+rome",
    "acropolis+of+athens",
    "pyeongchang",
    "pyongyang",
    "toronto+canada",
    "pyramids+of+giza",
    "singapore",
    "mt.+everest"
]

GOOGLE_MAPS = "com.google.android.apps.maps"

kill_flag = False


def start_google_maps_poi_search(search_query, trigger_event=True):
    if trigger_event:
        lf.trigger_new_event(search_query)
    lf.adb("am start -a \"android.intent.action.VIEW\" -d \"geo:0,0?q=" + search_query + "\"")


def record_maps_poi_search(search_query):
    start_google_maps_poi_search(search_query)
    time.sleep(config.DELAY_AFTER_LAUNCH)
    lf.kill_app(GOOGLE_MAPS)
    time.sleep(config.DELAY_AFTER_KILL)


# def check_config():
#   for url in TARGET_WEBSITES:
#      print("\nAttempt to launch " + url)
#     start_website(url)
#    time.sleep(1)


def setConfig(args):
    if args.permission_type[0] == "normal":
        lf.LOGGING_APP = lf.LOGGING_APP + lf.NORMAL_PERM
    elif args.permission_type[0] == "dangerous":
        lf.LOGGING_APP = lf.LOGGING_APP + lf.DANGEROUS_PERM
    elif args.permission_type[0] == "systemlevel":
        lf.LOGGING_APP = lf.LOGGING_APP + lf.SYSTEM_LEVEL_PERM


def init_maps():
    global kill_flag
    while not kill_flag:
        start_google_maps_poi_search(POI_SEARCH_QUERIES[0], trigger_event=False)
        time.sleep(3)
        if kill_flag:
            break
        start_google_maps_poi_search(POI_SEARCH_QUERIES[1], trigger_event=False)
        time.sleep(3)
        if kill_flag:
            break
        lf.kill_app(GOOGLE_MAPS)
    # print(subprocess.getoutput('adb logcat --regex=\[+\]'))


def main():
    global kill_flag
    parser = argparse.ArgumentParser(description='Recording websites')
    parser.add_argument('permission_type', nargs=1, help='Type of permissions to use. Possible values '
                                                         'are: normal, dangerous, systemlevel')

    setConfig(parser.parse_args())

    print("Recording POI searches starts\n")

    lf.start_logging_procedure()

    # We need to perform searches before the first actual event trigger to enable
    # the automated detection of side channel candidates
    thread = Thread(target=init_maps)
    thread.start()
    lf.wait_for_logcat(lf.LOGCAT_INIT_DONE)
    kill_flag = True
    print("Init of APIHarvester done")

    lf.kill_app(GOOGLE_MAPS)
    time.sleep(2)

    if not CHECK_CONFIGURATION:
        randomized_record.acquire_data_randomized(POI_SEARCH_QUERIES, config.RECORDS_PER_POI_SEARCH, record_maps_poi_search)
    # else:
    #    check_config()

    lf.stop_logging_app()


main()
