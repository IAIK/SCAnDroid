# The following code is based on the ProcHarvester implementation
# See https://github.com/IAIK/ProcHarvester/tree/master/code/adb-record-tools


import subprocess
import time
import config
import pexpect
import shutil
import datetime
import os
from threading import Thread

# Do not change these constants without changing the logging app!
LOGGING_APP = "at.tugraz.iaik.scandroid"
NORMAL_PERM = ".normalpermissions"
DANGEROUS_PERM = ".dangerouspermissions"
SYSTEM_LEVEL_PERM = ".systemlevelpermissions"
COMMAND_RECEIVE_ACTIVITY = "/at.tugraz.iaik.scandroid.services.CommandReceiverService"
COMMAND_KEY = "CMD"
ARG_KEY = "ARG"
CMD_START_LOGGING = "START_LOGGING"
CMD_STOP_LOGGING = "STOP_LOGGING"
CMD_TRIGGER_EVENT = "TRIGGER_EVENT"

LOGCAT_SYNC_MESSAGE = "--SYNC_MESSAGE--:"
LOGCAT_LOADING_DONE = "LOADING_DONE"
LOGCAT_INIT_DONE = "INIT_DONE"

WAIT_TIMEOUT = 15000000  # s


def adb(command, get_output=False):
    try:
        if get_output:
            return subprocess.getoutput('adb shell \"' + command + '\"')
        else:
            subprocess.run(['adb', 'shell', command])
    except FileNotFoundError:
        raise ConnectionError("adb connection failed")


def wait_for_logcat(sync_message, timeout=WAIT_TIMEOUT, logcat_sync_message=LOGCAT_SYNC_MESSAGE):
    try:
        # subprocess.run(['adb', 'logcat', "-c"])
        subprocess.run(['adb', 'logcat', '-G', '20m'])
        wait = True
        while wait:
            try:
                child = pexpect.spawn('adb logcat --regex=\"' + logcat_sync_message + '\" *:I')
                child.expect(logcat_sync_message + sync_message, timeout=timeout)
                wait = False
            except pexpect.EOF: # ignore adb EOF which happens if the device is printing too much
                wait = True
    except FileNotFoundError:
        raise ConnectionError("adb logcat connection failed")


def return_to_home_screen():
    adb("am start -a android.intent.action.MAIN -c android.intent.category.HOME")


def start_logging_app(clean_start=True):
    if clean_start:
        # clear logcat and kill logging app first since we need to return to home screen after sending commands
        subprocess.run(['adb', 'logcat', "-c"])
        kill_app(LOGGING_APP)
    send_command(CMD_START_LOGGING)


def start_logging_procedure(clean_start=True, return_to_home=True):
    #startRuntimeExceptionWatcherThread()
    start_logging_app(clean_start)
    wait_for_logcat(LOGCAT_LOADING_DONE)
    print("Loading of APIHarvester done")
    if return_to_home:
        return_to_home_screen()
    # time.sleep(config.DELAY_AFTER_KILL)


def stop_logging_app():
    send_command(CMD_STOP_LOGGING)
    time.sleep(config.DELAY_AFTER_KILL)
    return get_log()


def watchForRuntimeException():
    subprocess.run(['adb', 'logcat', "-c"])
    wait_for_logcat("FATAL EXCEPTION:", 99999999, "")
    print(
        "\033[91mRuntime exception has occurred on the phone! Something went wrong. Look into logcat for details.\033[0m")
    os._exit(-1)
    exit(-1)


def startRuntimeExceptionWatcherThread():
    thread = Thread(target=watchForRuntimeException)
    thread.start()


def get_log():
    # wait_for_logcat("")
    now = str(datetime.datetime.now())[:19]
    now = now.replace(":", "_")
    android_path = "/sdcard/Android/data/" + LOGGING_APP + "/files/"
    subprocess.run(['adb', 'pull', android_path])

    directory = "files/"
    source = os.listdir(directory)
    destination = "../analysis-tool/record_files/session_" + now + "/"
    os.mkdir(destination)
    for file in source:
        if file.endswith(".txt"):
            shutil.move(directory + file, destination + file)

    shutil.rmtree(directory)
    return destination


def trigger_new_event(target_label):
    send_command(CMD_TRIGGER_EVENT, target_label)


def send_command(command, argument=None):
    command_string = "am startservice -n " + LOGGING_APP + COMMAND_RECEIVE_ACTIVITY + " --es " + COMMAND_KEY + " " + command
    if argument is not None:
        command_string += " --es " + ARG_KEY + " " + argument
    print(command_string)
    adb(command_string)


def kill_app(package_name):
    adb("am force-stop " + package_name)


def launch_app(package_name, get_output=False, trigger_event=True):
    if trigger_event:
        trigger_new_event(package_name)
    return adb("monkey -p " + package_name + " -c android.intent.category.LAUNCHER 1", get_output)
