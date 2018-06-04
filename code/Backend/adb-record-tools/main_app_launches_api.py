# The following code is based on the ProcHarvester implementation
# See https://github.com/IAIK/ProcHarvester/tree/master/code/adb-record-tools

import logging_functions as lf
import time
import randomized_record
import config
import argparse
from threading import Thread

kill_flag = False


def start_stop_target_app(package_name, trigger_event=True):
    lf.launch_app(package_name, trigger_event=trigger_event)
    time.sleep(config.DELAY_AFTER_LAUNCH)
    lf.kill_app(package_name)
    time.sleep(config.DELAY_AFTER_KILL)


def acquire_resume_data(labels, records_per_app):
    print("\nExecute app resume sequence")
    for target_label in labels:
        do_resume_sequence(target_label, records_per_app)


def do_resume_sequence(package_name, records_per_app):
    # Do not trigger event at first launch since this is a cold start
    lf.launch_app(package_name, trigger_event=False)
    time.sleep(config.DELAY_AFTER_LAUNCH)
    lf.return_to_home_screen()
    time.sleep(config.DELAY_AFTER_LAUNCH)

    for cnt in range(1, records_per_app + 1):
        print('\nResume Launch ' + str(cnt) + ': ' + package_name)
        lf.launch_app(package_name, trigger_event=True)
        time.sleep(config.DELAY_AFTER_LAUNCH)
        lf.return_to_home_screen()
        time.sleep(config.DELAY_AFTER_KILL)

    lf.kill_app(package_name)
    time.sleep(config.DELAY_AFTER_KILL)


def init_websites():
    global kill_flag
    while not kill_flag:
        start_stop_target_app(config.TARGET_APPS[0], trigger_event=False)
        time.sleep(2)
        if kill_flag:
            break
        start_stop_target_app(config.TARGET_APPS[1], trigger_event=False)
        time.sleep(2)


def setConfig(args):
    if args.permission_type[0] == "normal":
        lf.LOGGING_APP = lf.LOGGING_APP + lf.NORMAL_PERM
    elif args.permission_type[0] == "dangerous":
        lf.LOGGING_APP = lf.LOGGING_APP + lf.DANGEROUS_PERM
    elif args.permission_type[0] == "systemlevel":
        lf.LOGGING_APP = lf.LOGGING_APP + lf.SYSTEM_LEVEL_PERM


def main():
    global kill_flag
    parser = argparse.ArgumentParser(description='Recording app launches')
    parser.add_argument('permission_type', nargs=1, help='Type of permissions to use. Possible values '
                                                         'are: normal, dangerous, systemlevel')
    setConfig(parser.parse_args())

    print('Recording app launches (', config.TARGET_APPS, ')')
    print("Record in mode: " + str(config.RECORD_MODE) + "\n")

    lf.start_logging_procedure()

    thread = Thread(target=init_websites)
    thread.start()
    lf.wait_for_logcat(lf.LOGCAT_INIT_DONE)
    kill_flag = True
    print("Init of APIHarvester done")

    if config.RECORD_MODE == config.RecordMode.MIXED_MODE:
        # The first starts for resume launches do not qualify as actual app resumes
        COLD_START_RECORDS_PER_APP = 7
        RESUME_RECORDS_PER_APP = 9
        randomized_record.acquire_data_randomized(config.TARGET_APPS, COLD_START_RECORDS_PER_APP, start_stop_target_app)
        acquire_resume_data(config.TARGET_APPS, RESUME_RECORDS_PER_APP)
    elif config.RECORD_MODE == config.RecordMode.APP_RESUMES:
        acquire_resume_data(config.TARGET_APPS, config.records_per_app())
    elif config.RECORD_MODE == config.RecordMode.COLD_STARTS:
        randomized_record.acquire_data_randomized(config.TARGET_APPS, config.records_per_app(), start_stop_target_app)

    destination_folder = lf.stop_logging_app()


main()
