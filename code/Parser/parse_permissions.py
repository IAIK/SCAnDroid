import re

import os
from lxml import html
import requests
from bs4 import BeautifulSoup
import sys

folder_names = ["normalpermissions", "dangerouspermissions", "systemlevelpermissions"]


def write(files, data, type):
    for index, file in enumerate(files):
        if (index == 0 and ("normal" in type or type == ""))\
                or (index == 1 and ("normal" in type or "dangerous" in type or type == ""))\
                or index == 2:  # "signature" "privileged" "dangerous" "normal"
            file.write(data)


def create_files():
    files = []
    for folder_name in folder_names:
        if not os.path.exists(folder_name):
            os.makedirs(folder_name)
        files.append(open(folder_name + "/AndroidManifest.xml", "w"))
    return files


def create_manifest(permission_names, protection_levels):
    assert (len(permission_names) == len(protection_levels))

    files = create_files()

    write(files, "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n", "")
    write(files, "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n", "")
    for permission_name, protection_level in zip(permission_names, protection_levels):
        if all(map(str.isupper, re.sub('[_]', '', permission_name))):  # Only print uppercase constants (removes e.g. Manifest.permission)
            write(files, "    <uses-permission android:name=\"android.permission." + permission_name + "\" />\n", protection_level)
    write(files, "</manifest>", "")
    for file in files:
        file.close()


def main(argv):
    #apilevel = 26
    #if apilevel == -1:
    #    selector = "api"
    #else:
    #    selector = "apilevel-" + str(apilevel)

    main_page = requests.get('https://developer.android.com/reference/android/Manifest.permission.html')
    main_tree = html.fromstring(main_page.content)
    permission_names = main_tree.xpath('//div[contains(@class,"api")]/h3[@class="api-name"]/text()')

    soup = BeautifulSoup(main_page.text, 'html.parser')

    permission_descriptions = soup.select("div.api")

    protection_levels = []
    for permission_description in permission_descriptions:
        parsed_protection_level = [line.strip() for line in permission_description.text.split('\n') if
                                   "Protection level: " in line]
        if len(parsed_protection_level) > 1:
            continue
        if len(parsed_protection_level) == 0:
            parsed_protection_level = [line.strip() for line in permission_description.text.split('\n') if
                                       "Not for use by third-party applications." in line]
        if len(parsed_protection_level) == 0:
            parsed_protection_level = [line.strip() for line in permission_description.text.split('\n') if
                                       "This is not available to third party applications" in line]
        if len(parsed_protection_level) == 0:
            parsed_protection_level = [line.strip() for line in permission_description.text.split('\n') if
                                       "can grant permission through the Settings application" in line]
        if len(parsed_protection_level) > 0:
            protection_levels.append(parsed_protection_level[0])
        else:
            protection_levels.append("")

    create_manifest(permission_names, protection_levels)


main(sys.argv)
