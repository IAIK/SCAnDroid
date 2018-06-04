from lxml import html
import requests
from bs4 import BeautifulSoup
#import lxml, lxml.html
        #print(lxml.html.tostring(childtree))

def parse_methods(class_tree, file, selector, tableId, methodType):
    possible_parameters_of_all_methods = class_tree.xpath(
        '//table[@id="' + tableId + '"]/tr[' + selector + ']/td[@width="100%"]/code/text()')
    possible_function_names = class_tree.xpath(
        '//table[@id="' + tableId + '"]/tr[' + selector + ']/td[@width="100%"]/code/a[1]/text()')
    possible_parameters_of_all_methods = ("".join(possible_parameters_of_all_methods)).split("\n")

    # file.write(name + "." + classname + " | " + str(len(possible_function_names)) + "\n")
    file.write(" " + methodType + ": " + str(len(possible_function_names)) + "\n")

    to_remove = ['(', ')', '<', '>']
    #primitive_datatypes = ["byte", "short", "boolean", "int", "float", "double", "long", "char",
    #                       "byte[]", "short[]", "boolean[]", "int[]", "float[]", "double[]", "long[]",
    #                       "char[]"]

    method_name_ctr = 0

    for possible_parameters_of_methods in possible_parameters_of_all_methods:
        possible_parameters_of_methods = possible_parameters_of_methods.strip().split("\n")

        for possible_parameters_of_method in possible_parameters_of_methods:
            if possible_parameters_of_method != "":
                if len(possible_function_names) > method_name_ctr:
                    file.write("  " + possible_function_names[method_name_ctr] + " | ")
                    method_name_ctr += 1
                possible_parameters_of_method = ''.join(c for c in possible_parameters_of_method if not c in to_remove)
                splitted_possible_parameters_of_method = possible_parameters_of_method.split(',')
                result_words = [word.split(' ')[-1] for word in splitted_possible_parameters_of_method]
                #result_words = [word for word in possible_parameters_of_method.split() if
                #                word.lower() not in primitive_datatypes]
                possible_parameters_of_method = ', '.join(result_words)
                file.write(possible_parameters_of_method + "\n")


def main():
    # apilevel = -1: parse all methods/constructors
    # apilevel = 26 and not upto_apilevel: only parse methods/constructors added in API v26
    # apilevel = 26 and upto_apilevel: parse methods/constructors up to (including) API v26
    # max_apilevel: upper limit (inclusive)
    apilevel = -1
    max_apilevel = 27
    upto_apilevel = True
    if upto_apilevel and not apilevel == -1:
        apilevel += 1
        if apilevel <= max_apilevel:
            selector = "not(contains(@class,\"apilevel-" + str(apilevel) + "\"))"
        apilevel += 1
        while apilevel <= max_apilevel:
            selector += " and " + "not(contains(@class,\"apilevel-" + str(apilevel) + "\"))"
            apilevel += 1
    else:
        if apilevel == -1:
            selector = "contains(@class,\"api\")"
        else:
            selector = "contains(@class,\"apilevel-" + str(apilevel) + "\")"

    print(selector)

    mainpage = requests.get('https://developer.android.com/reference/packages.html')
    maintree = html.fromstring(mainpage.content)
    mainlinks = maintree.xpath('//td[@class="jd-linkcol"]/a/@href')
    mainname = maintree.xpath('//td[@class="jd-linkcol"]/a/text()')

    constructor_file = open("constructors.txt", "w")
    methods_file = open("methods.txt", "w")
    #constants_file = open("constants.txt", "w")
    

    for index in range(0, len(mainlinks)):
        if index % 10 == 0:
            print(str(index) + " of " + str(len(mainlinks)) + " parsed")
        link = mainlinks[index]
        name = mainname[index]
        childpage = requests.get(link)
        childtree = html.fromstring(childpage.content)
        classnames = childtree.xpath('//td[@class="jd-linkcol"]/a[1]/text()')
        childlinks = childtree.xpath('//td[@class="jd-linkcol"]/a[1]/@href')
        #print(classnames)
        #print(childlinks)
        for classname, childlink in zip(classnames, childlinks):
            classpage = requests.get(childlink)
            classtree = html.fromstring(classpage.content)
            classname = classname.replace('.', '$')
            constructor_file.write(name + "." + classname + "\n")
            methods_file.write(name + "." + classname + "\n")
            #constants_file.write(name + "." + classname + "\n")
            parse_methods(classtree, constructor_file, selector, "pubctors", "Constructors")
            parse_methods(classtree, methods_file, selector, "pubmethods", "Methods")
            #parse_methods(classtree, constants_file, selector, "constants", "Constants")

    constructor_file.close()
    methods_file.close()
    #constants_file.close()


main()
