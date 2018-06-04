package at.tugraz.iaik.scandroid;

import android.content.ContextWrapper;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import at.tugraz.iaik.scandroid.parsedTypes.APIClassInfo;
import at.tugraz.iaik.scandroid.parsedTypes.APIMethodInfo;
import at.tugraz.iaik.scandroid.types.CustomHashMap;

/**
 * Created by Gerald Palfinger on 27.11.17.
 */

public class Parser {
    private static final boolean DEBUG = false;
    private final String TAG = Parser.class.getName();

    private final CustomHashMap<String, APIClassInfo> methodsOfClassesToCheckParsed = new CustomHashMap<>();
    private final CustomHashMap<String, APIClassInfo> methodsOfAllClassesParsed = new CustomHashMap<>();
    private final HashMap<String, ArrayList<String[]>> constructorsOfClassesToCheckParsed = new HashMap<>();
    private final HashMap<String, ArrayList<String[]>> constructorsOfAllClassesParsed = new HashMap<>();

    public Parser(ContextWrapper contextWrapper) throws IOException {
        parseMethodsAndParameters(contextWrapper, Config.methods_file_resource, methodsOfClassesToCheckParsed);
        parseMethodsAndParameters(contextWrapper, Config.all_methods_file_resource, methodsOfAllClassesParsed);
        parseConstructorsAndParameters(contextWrapper, Config.constructors_file_resource, constructorsOfClassesToCheckParsed, methodsOfClassesToCheckParsed);
        parseConstructorsAndParameters(contextWrapper, Config.all_constructors_file_resource, constructorsOfAllClassesParsed, methodsOfAllClassesParsed);

        int methods = 0;
        for (Map.Entry<String, APIClassInfo> clasz : methodsOfClassesToCheckParsed.entrySet()) {
            for (Map.Entry<String, APIMethodInfo> method : clasz.getValue().methodInfos.entrySet()) {
                methods += method.getValue().getParameterNames().size();
            }
        }

        Log.v(TAG, "--------- PARSING INFO ---------");
        Log.v(TAG, "methods + constructors parsed = " + methods);
        Log.v(TAG, "methodsOfClassesToCheckParsed.size() = " + methodsOfClassesToCheckParsed.size());
        Log.v(TAG, "methodsOfAllClassesParsed.size() = " + methodsOfAllClassesParsed.size());
        Log.v(TAG, "constructorsOfClassesToCheckParsed.size() = " + constructorsOfClassesToCheckParsed.size());
        Log.v(TAG, "constructorsOfAllClassesParsed.size() = " + constructorsOfAllClassesParsed.size());
        Log.v(TAG, "methodsOfClassesToCheckParsed = " + methodsOfClassesToCheckParsed);
        Log.v(TAG, "methodsOfAllClassesParsed = " + methodsOfAllClassesParsed);
        Log.v(TAG, "constructorsOfClassesToCheckParsed = " + constructorsOfClassesToCheckParsed);
        Log.v(TAG, "constructorsOfAllClassesParsed = " + constructorsOfAllClassesParsed);
        Log.v(TAG, "--------- PARSING DONE ---------");
    }

    private void parseConstructorsAndParameters(ContextWrapper contextWrapper, final int constructors_resource,
                                                HashMap<String, ArrayList<String[]>> constructorsAndParametersToUse,
                                                CustomHashMap<String, APIClassInfo> methodsMap) throws IOException {
        BufferedReader constructorsReader = new BufferedReader(new InputStreamReader(contextWrapper.getResources().openRawResource(constructors_resource)));
        String currentClass;

        do {
            currentClass = constructorsReader.readLine();
            if (currentClass == null) {
                return;
            }
            int nrOfMethods = getNrOfMethods(constructorsReader, "Constructors");
            HashMap<String, ArrayList<String[]>> constructorAndParameters = parseMethodsAndParametersOfSingleClass(constructorsReader, currentClass, nrOfMethods, methodsMap);
            if (constructorAndParameters.entrySet().size() > 1) {
                throw new RuntimeException(Config.LOGCAT_RUNTIME_ERROR + "Input file malformed: constructorAndParameters.entrySet().size() > 1");
            }
            for (Map.Entry<String, ArrayList<String[]>> constructor : constructorAndParameters.entrySet()) {
                constructorsAndParametersToUse.put(currentClass, constructor.getValue());
            }
        } while (true);
    }

    private void parseMethodsAndParameters(ContextWrapper contextWrapper, final int methodsResource, CustomHashMap<String, APIClassInfo> methodsOfClasses) throws IOException {
        BufferedReader methodsReader = new BufferedReader(new InputStreamReader(contextWrapper.getResources().openRawResource(methodsResource)));
        int totalMethods = 0;
        do {
            String currentClass = methodsReader.readLine();
            if (DEBUG)
                Log.v(TAG, "Parsing class: " + currentClass);
            if (currentClass == null) {
                break;
            }

            if (currentClass.startsWith(" ") || currentClass.startsWith("  ")) {
                // should not happen here
                throw new RuntimeException(Config.LOGCAT_RUNTIME_ERROR + "Input file malformed. Should both be false: " +
                        "currentClass.startsWith(\" \") + currentClass.startsWith(\"  \") = " + currentClass.startsWith(" ") + currentClass.startsWith("  "));
            } else {
                // new class
                int nrOfMethods = getNrOfMethods(methodsReader, "Methods");
                totalMethods += nrOfMethods;
                parseMethodsAndParametersOfSingleClass(methodsReader, currentClass, nrOfMethods, methodsOfClasses);
            }
        } while (true);
        Log.v(TAG, "totalMethods = " + totalMethods);
    }

    @NonNull
    //currentClass e.g.: currentClass = java.lang.RuntimeException
    private CustomHashMap<String, ArrayList<String[]>> parseMethodsAndParametersOfSingleClass(BufferedReader reader, String currentClass, int nrOfMethods, CustomHashMap<String, APIClassInfo> methodsOfClasses) throws IOException {
        String line;
        // method name --> arraylist (in case there are more methods with the same name) --> string array of parameters
        CustomHashMap<String, ArrayList<String[]>> methodsAndParameters = new CustomHashMap<>();
        methodsOfClasses.putIfAbsent(currentClass, new APIClassInfo(currentClass));
        APIClassInfo apiClassInfo = methodsOfClasses.get(currentClass);
        for (int methodCounter = 0; methodCounter < nrOfMethods; ++methodCounter) {
            line = reader.readLine();
            if (!line.startsWith("  ")) {
                throw new RuntimeException(Config.LOGCAT_RUNTIME_ERROR + "Input file malformed: !line.startsWith(\"  \")");
            }
            String[] methodNameAndParameters = line.trim().split("\\|");
            String methodName = methodNameAndParameters[0].trim();

            if (methodNameAndParameters.length > 1) {
                apiClassInfo.addMethod(methodName, methodNameAndParameters[1].trim().split(", "));
            } else {
                apiClassInfo.addMethod(methodName);
            }

            methodsAndParameters.putIfAbsent(methodName, new ArrayList<>());
            methodsAndParameters.get(methodName).add(
                    (methodNameAndParameters.length > 1) ? methodNameAndParameters[1].trim().split(", ") : new String[0]);
            if (methodNameAndParameters.length > 1 && DEBUG)
                Log.v(TAG, "class " + currentClass + " method " + methodName + " methodsAndParameters.get(methodNameAndParameters[0]).size() = " + methodsAndParameters.get(methodName).size() + " array:" + Arrays.toString(methodNameAndParameters[1].trim().split(",")));
        }
        return methodsAndParameters;
    }

    public static int getNrOfMethods(BufferedReader reader, String type) throws IOException {
        String line = reader.readLine();
        String[] typeAndCount = line.trim().split(":");
        if (typeAndCount.length != 2 || !typeAndCount[0].equals(type)) {
            throw new RuntimeException(Config.LOGCAT_RUNTIME_ERROR + Config.LOGCAT_RUNTIME_ERROR + "Input file malformed. Should both be false: typeAndCount.length != 2 + !typeAndCount[0].equals(type) = " + (typeAndCount.length != 2) + !typeAndCount[0].equals(type));
        }
        return Integer.parseInt(typeAndCount[1].trim());
    }

    CustomHashMap<String, APIClassInfo> getMethodsOfClassesToCheckParsed() {
        return methodsOfClassesToCheckParsed;
    }

    HashMap<String, ArrayList<String[]>> getConstructorsOfClassesToCheckParsed() {
        return constructorsOfClassesToCheckParsed;
    }

    CustomHashMap<String, APIClassInfo> getMethodsOfAllClassesParsed() {
        return methodsOfAllClassesParsed;
    }

    HashMap<String, ArrayList<String[]>> getConstructorsOfAllClassesParsed() {
        return constructorsOfAllClassesParsed;
    }
}
