# The following code is based on the ProcHarvester implementation
# See https://github.com/IAIK/ProcHarvester/tree/master/code/adb-record-tools

import argparse
import logging_functions as lf
import randomized_record
import time
import config
from threading import Thread

CHECK_CONFIGURATION = False
BROWSER = ""
CHROMIUM = "org.chromium.chrome"
FENNEC = "org.mozilla.fennec_fdroid"
KLAR = "org.mozilla.klar"
CHROME = "com.android.chrome"
FIREFOX = "org.mozilla.firefox"

kill_flag = False


def start_website(url, trigger_event=True):
    if trigger_event:
        lf.trigger_new_event(url)
    lf.adb("am start -a \"android.intent.action.VIEW\" -d \"" + url + "\"")


def record_website(url):
    start_website(url)
    time.sleep(config.DELAY_AFTER_LAUNCH)
    lf.kill_app(BROWSER)
    time.sleep(config.DELAY_AFTER_KILL)


def check_config():
    for url in config.TARGET_WEBSITES:
        print("\nAttempt to launch " + url)
        start_website(url)
        time.sleep(1)


def setConfig(args):
    global BROWSER
    if args.browser[0] == "klar":
        BROWSER = KLAR
    elif args.browser[0] == "fennec":
        BROWSER = FENNEC
    elif args.browser[0] == "chromium":
        BROWSER = CHROMIUM
    elif args.browser[0] == "chrome":
        BROWSER = CHROME
    elif args.browser[0] == "firefox":
        BROWSER = FIREFOX
    else:
        print("Unknown browser specified")
        exit(-1)

    if args.permission_type[0] == "normal":
        lf.LOGGING_APP = lf.LOGGING_APP + lf.NORMAL_PERM
    elif args.permission_type[0] == "dangerous":
        lf.LOGGING_APP = lf.LOGGING_APP + lf.DANGEROUS_PERM
    elif args.permission_type[0] == "systemlevel":
        lf.LOGGING_APP = lf.LOGGING_APP + lf.SYSTEM_LEVEL_PERM


def init_websites():
    global kill_flag
    while not kill_flag:
        start_website(config.TARGET_WEBSITES[0], trigger_event=False)
        time.sleep(5)
        if kill_flag:
            break
        start_website(config.TARGET_WEBSITES[1], trigger_event=False)
        time.sleep(4)
        if kill_flag:
            break
        lf.kill_app(BROWSER)
    #print(subprocess.getoutput('adb logcat --regex=\[+\]'))


def main():
    global kill_flag
    parser = argparse.ArgumentParser(description='Recording websites')
    parser.add_argument('permission_type', nargs=1, help='Type of permissions to use. Possible values '
                                                         'are: normal, dangerous, systemlevel')
    parser.add_argument('browser', nargs=1, help='Browser name. Possible values '
                                                 'are: fennec, firefox, klar, chromium, chrome')
    setConfig(parser.parse_args())

    print("Record website starts\n")

    lf.start_logging_procedure()

    # We need to perform website launches before the first actual event trigger to enable
    # the automated detection of side channel candidates
    thread = Thread(target=init_websites)
    thread.start()
    lf.wait_for_logcat(lf.LOGCAT_INIT_DONE)
    kill_flag = True
    print("Init of APIHarvester done")
    lf.kill_app(BROWSER)
    time.sleep(2)

    if not CHECK_CONFIGURATION:
        assert len(config.TARGET_WEBSITES) == 20
        randomized_record.acquire_data_randomized(config.TARGET_WEBSITES, config.RECORDS_PER_WEBSITE, record_website)
    else:
        check_config()

    lf.stop_logging_app()


main()
