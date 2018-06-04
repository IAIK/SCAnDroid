import argparse

prefixes = ["get", "query", "has", "is", "nativeGet", "nativeQuery", "nativeHas", "nativeIs"]


def relevant(str):
    for prefix in prefixes:
        if str.startswith(prefix):
            return True
    return False

parser = argparse.ArgumentParser(description='Recording websites')
parser.add_argument('methods_file', nargs=1, help='path to methods file')
file_path = parser.parse_args().methods_file[0]
file = open(file_path, "r")
methods = 0
methods_as_counted_by_parser = 0
relevant_methods = 0

for line in file:
    if len(line) > 2 and line[0] == ' ' and line[1] == ' ':
        methods += 1
        splitted_line = line.split("|")
        if relevant(splitted_line[0].strip()):
            relevant_methods += 1
        
    splitted_line = line.split(":")
    if len(splitted_line) == 2:
        methods_as_counted_by_parser += int(splitted_line[1].replace("\n", "").strip())

assert(methods == methods_as_counted_by_parser)
print("The file contains", methods, "methods.")
print("Possibly relevant metohds:", relevant_methods)