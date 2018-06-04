# The following code is based on the ProcHarvester implementation
# See https://github.com/IAIK/ProcHarvester/tree/master/code/adb-record-tools

import argparse
import time
import randomized_record
import config
import logging_functions as lf


def ask_for_user_action(command):
    input(command + " - press [ENTER] to continue...")
    print("...")


def instruct_manual_launch(package_name):
    ask_for_user_action("\nStart " + package_name)
    lf.trigger_new_event(package_name)
    time.sleep(config.DELAY_AFTER_LAUNCH)
    ask_for_user_action("Kill foreground app")
    time.sleep(config.DELAY_AFTER_KILL)


def setConfig(args):
    if args.permission_type[0] == "normal":
        lf.LOGGING_APP = lf.LOGGING_APP + lf.NORMAL_PERM
    elif args.permission_type[0] == "dangerous":
        lf.LOGGING_APP = lf.LOGGING_APP + lf.DANGEROUS_PERM
    elif args.permission_type[0] == "systemlevel":
        lf.LOGGING_APP = lf.LOGGING_APP + lf.SYSTEM_LEVEL_PERM


def main():
    parser = argparse.ArgumentParser(description='Recording app launches')
    parser.add_argument('permission_type', nargs=1, help='Type of permissions to use. Possible values '
                                                         'are: normal, dangerous, systemlevel')
    setConfig(parser.parse_args())

    print("Record websites manually\n")
    ask_for_user_action("Start exploration phase\n")
    print("Please open different websites on the phone until instructed otherwise")

    lf.start_logging_procedure()
    lf.wait_for_logcat(lf.LOGCAT_INIT_DONE)
    print("Init of APIHarvester done")
    print("Stop opening websites")
    time.sleep(2)

    ask_for_user_action("Start logging procedure\n")
    randomized_record.acquire_data_randomized(config.TARGET_WEBSITES, config.RECORDS_PER_WEBSITE, instruct_manual_launch)
    ask_for_user_action("\nStop logging app")

    destination_folder = lf.stop_logging_app()


main()
