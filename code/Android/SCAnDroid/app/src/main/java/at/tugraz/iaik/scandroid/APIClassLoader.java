package at.tugraz.iaik.scandroid;

import android.content.ContextWrapper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import at.tugraz.iaik.scandroid.types.APIClass;
import at.tugraz.iaik.scandroid.parsedTypes.APIClassInfo;
import at.tugraz.iaik.scandroid.types.APIMethod;
import at.tugraz.iaik.scandroid.parsedTypes.APIMethodInfo;
import at.tugraz.iaik.scandroid.types.CustomHashMap;

/**
 * Created by Gerald Palfinger on 27.10.17.
 */
public class APIClassLoader {
    private static final boolean LOGGING = false;
    private final String TAG = APIClassLoader.class.getName();

    private final CustomHashMap<String, APIClass> methodsOfClasses = new CustomHashMap<>();
    private final ContextWrapper contextWrapper;
    private final Predefines predefines;
    private final Parser parser;
    private final CustomHashMap<String, APIClassInfo> methodsOfClassesToCheckParsed; // methods to invoke in the first exploration step (from Config.[methods|constructors]_file_resource)
    private final CustomHashMap<String, APIClassInfo> methodsOfAllClassesParsed; // for parameter creation and return value exploration (from Config.all_[methods|constructors]_file_resource)

    // COVERAGE / DEBUG INFORMATION
    private FileOutputStream abstractOrInterfacesMethodsStream;
    private FileOutputStream exceptionWhileCreationStream;
    private int addedPrimitiveReturnMethods;
    private int allMethods;
    private int notInvokableMethods;
    private FileOutputStream notInvokableMethodsFileStream;
    private FileOutputStream invokableMethodsReturningPrimitiveFileStream;
    private FileOutputStream invokableMethodsReturningObjectFileStream;
    private FileOutputStream notFoundClassesStream;
    private FileOutputStream notFoundClassesMoreInformationStream;
    private FileOutputStream abstractOrInterfacesStream;
    private FileOutputStream errorClassesStream;
    private int possiblyExploredObjectReturnMethod;
    private FileOutputStream illegalAccessMethodStream;
    private FileOutputStream illegalArgumentMethodStream;
    private FileOutputStream invocationTargetMethodStream;
    private FileOutputStream nullPointerMethodStream;
    private FileOutputStream classCreationExceptionStream;
    private FileOutputStream parameterCreationExceptionStream;
    private FileOutputStream checkMethodStream;
    private FileOutputStream noClassObject;

    private int methodsWhichWeCreatedObjectFor;
    private int relAndPubMethodsTotal;
    //private int first_if;
    //private boolean calledFromHere;
    private int blacklist;
    private int methodDoesNotExist;
    private int missedMethodsDueToClassCreationError;
    private int missedMethodsDueToNotFound;
    private int missedMethodsDueToClassCreationErrorSingle;
    private int missedMethodsDueToNotFoundSingle;
    //private int created_parameter_object;
    //private int have_class_object;
    private int missedMethodsDueAbstractInterface;
    private int missedMethodsDueToNoObject;
    private FileOutputStream missedMethodsDueToNoObjectStream;
    private int exceptionWhileCreation;
    private int missedMethodsDueToNoObjectParam;

    public CustomHashMap<String, APIClass> getMethodsOfClasses() {
        return methodsOfClasses;
    }

    public APIClassLoader(ContextWrapper contextWrapper, Predefines predefines,
                          Parser parser) throws IOException {

        initDebugFiles(contextWrapper);

        this.contextWrapper = contextWrapper;
        this.predefines = predefines;
        this.parser = parser;
        methodsOfClassesToCheckParsed = parser.getMethodsOfClassesToCheckParsed();
        methodsOfAllClassesParsed = parser.getMethodsOfAllClassesParsed();

        loadParsedClasses();
        loadPredefinedClasses(contextWrapper);

        writeDebugInfo();
    }

    // writes various debug data to files (e.g., for coverage)
    private void writeDebugInfo() throws IOException {
        Log.v(TAG, "notInvokableMethods = " + notInvokableMethods);
        Log.v(TAG, "addedPrimitiveReturnMethods = " + addedPrimitiveReturnMethods);
        Log.v(TAG, "allMethods = " + allMethods);
        Log.v(TAG, "possiblyExploredObjectReturnMethod = " + possiblyExploredObjectReturnMethod);
        Log.v(TAG, "methodsWhichWeCreatedObjectFor = " + methodsWhichWeCreatedObjectFor);
        checkMethodStream.write(("\n\nrelAndPubMethodsTotal: " + relAndPubMethodsTotal + "\nmethodsWhichWeCreatedObjectFor: " + methodsWhichWeCreatedObjectFor +
                //"\nfirst_if: " + first_if
                "\nblacklist: " + blacklist + "\nmethodDoesNotExist: " + methodDoesNotExist
                //+ "\ncreated_parameter_object: " + created_parameter_object + "\nhave_class_object: " + have_class_object + "\n"
        ).getBytes());
        classCreationExceptionStream.write(("\n\nmissedMethodsDueToClassCreationError: " + missedMethodsDueToClassCreationError + " (" + missedMethodsDueToClassCreationErrorSingle + ")").getBytes());
        notFoundClassesMoreInformationStream.write(("\nmissedMethodsDueToNotFound: " + missedMethodsDueToNotFound + " (" + missedMethodsDueToNotFoundSingle + ")").getBytes());
        abstractOrInterfacesMethodsStream.write(("\n\n missedMethodsDueAbstractInterface: " + missedMethodsDueAbstractInterface).getBytes());
        missedMethodsDueToNoObjectStream.write(("\n\nmissedMethodsDueToNoObject:" + missedMethodsDueToNoObject + "\nmissedMethodsDueToNoObjectParam: " + missedMethodsDueToNoObjectParam + "\n").getBytes());
        exceptionWhileCreationStream.write(("\n\nexceptionWhileCreation: " + exceptionWhileCreation).getBytes());
    }

    // init various debug files
    private void initDebugFiles(ContextWrapper contextWrapper) throws FileNotFoundException {
        File notInvokableMethodsFile = new File(contextWrapper.getExternalFilesDir(null), "not_invokable_methods.info");
        File invokableMethodsReturningPrimitiveFile = new File(contextWrapper.getExternalFilesDir(null), "invokable_methods_primitive_return.info");
        File invokableMethodsReturningObjectFile = new File(contextWrapper.getExternalFilesDir(null), "invokable_methods_object_return_possibly_explored_for_methods.info");
        notInvokableMethodsFileStream = new FileOutputStream(notInvokableMethodsFile);
        invokableMethodsReturningPrimitiveFileStream = new FileOutputStream(invokableMethodsReturningPrimitiveFile);
        invokableMethodsReturningObjectFileStream = new FileOutputStream(invokableMethodsReturningObjectFile);
        File notFoundClasses = new File(contextWrapper.getExternalFilesDir(null), "not_found_classes.info");
        File errorClasses = new File(contextWrapper.getExternalFilesDir(null), "error_classes.info");
        notFoundClassesStream = new FileOutputStream(notFoundClasses);
        errorClassesStream = new FileOutputStream(errorClasses);

        File illegalAccessMethodFile = new File(contextWrapper.getExternalFilesDir(null), "method_invocation_illegalAccess.info");
        illegalAccessMethodStream = new FileOutputStream(illegalAccessMethodFile);
        File illegalArgumentMethodFile = new File(contextWrapper.getExternalFilesDir(null), "method_invocation_illegalArgument.info");
        illegalArgumentMethodStream = new FileOutputStream(illegalArgumentMethodFile);
        File invocationTargetMethodFile = new File(contextWrapper.getExternalFilesDir(null), "method_invocation_invocationTarget.info");
        invocationTargetMethodStream = new FileOutputStream(invocationTargetMethodFile);
        File nullPointerMethodFile = new File(contextWrapper.getExternalFilesDir(null), "method_invocation_nullPointer.info");
        nullPointerMethodStream = new FileOutputStream(nullPointerMethodFile);

        File classCreationExceptionFile = new File(contextWrapper.getExternalFilesDir(null), "class_creation_exception_other_than_not_found.info");
        classCreationExceptionStream = new FileOutputStream(classCreationExceptionFile);

        File abstractOrInterfacesFile = new File(contextWrapper.getExternalFilesDir(null), "abstract_classes_and_interfaces.info");
        abstractOrInterfacesStream = new FileOutputStream(abstractOrInterfacesFile);

        File abstractOrInterfacesMethodsFile = new File(contextWrapper.getExternalFilesDir(null), "abstract_classes_and_interfaces_methods.info");
        abstractOrInterfacesMethodsStream = new FileOutputStream(abstractOrInterfacesMethodsFile);

        File parameterCreationExceptionFile = new File(contextWrapper.getExternalFilesDir(null), "parameter_creation_exceptions.info");
        parameterCreationExceptionStream = new FileOutputStream(parameterCreationExceptionFile);

        File checkMethodsFile = new File(contextWrapper.getExternalFilesDir(null), "check_methods.info");
        checkMethodStream = new FileOutputStream(checkMethodsFile);

        File notFoundClassesMoreInformationFile = new File(contextWrapper.getExternalFilesDir(null), "not_found_classes_more_info.info");
        notFoundClassesMoreInformationStream = new FileOutputStream(notFoundClassesMoreInformationFile);

        File noClassObjectFile = new File(contextWrapper.getExternalFilesDir(null), "no_class_object_created.info");
        noClassObject = new FileOutputStream(noClassObjectFile);

        File missedMethodsDueToNoObjectFile = new File(contextWrapper.getExternalFilesDir(null), "missedMethodsDueToNoObject.info");
        missedMethodsDueToNoObjectStream = new FileOutputStream(missedMethodsDueToNoObjectFile);

        File exceptionWhileCreationFile = new File(contextWrapper.getExternalFilesDir(null), "constructorCallExceptionWhileCreation.info");
        exceptionWhileCreationStream = new FileOutputStream(exceptionWhileCreationFile);
    }

    // exploration phase
    private void saveInvokableMethods(Class<?> clazz, ArrayList<Object> classObjects, int currentRecursionDepth, String currentClass,
                                      Object parentClassObject, Method objectCreatorMethod, Object[] objectCreatorMethodParameters,
                                      CustomHashMap<String, APIClassInfo> methodsOfClassesParsed, String hierarchy) {
        if (LOGGING) {
            Log.v(TAG, "currentRecursionDepth = " + currentRecursionDepth);
        }
        if (currentRecursionDepth == 0) {
            try {
                int relAndPubMethods = 0;
                for (Method methodToInvoke : clazz.getDeclaredMethods()) {
                    if (Config.isRelevantMethod(methodToInvoke.getName()) && Modifier.isPublic(methodToInvoke.getModifiers())) {
                        relAndPubMethods++;
                    }
                }
                relAndPubMethodsTotal += relAndPubMethods;
                checkMethodStream.write(("  relevant and public methods according to reflection: " + Integer.toString(relAndPubMethods) + "\n").getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (Method methodToInvoke : clazz.getDeclaredMethods()) {
            boolean addedMethod = false;
            // Allow calling non-public methods
            methodToInvoke.setAccessible(true);
            APIClassInfo apiClassInfo = methodsOfClassesParsed.getOrDefault(currentClass, null);
            /*if (calledFromHere && currentRecursionDepth == 0 && apiClassInfo == null) {
                throw new RuntimeException();
            }*/

            if (currentRecursionDepth == 0 && Config.METHOD_BLACKLIST.contains(methodToInvoke.getName())) {
                blacklist++;
            }
            if (currentRecursionDepth == 0 && apiClassInfo != null &&
                    Config.isRelevantMethod(methodToInvoke.getName()) && !Config.METHOD_BLACKLIST.contains(methodToInvoke.getName())
                    && !apiClassInfo.methodExists(methodToInvoke.getName())) {
                methodDoesNotExist++;
            }
            //calledFromHere = false;

            if (Config.isRelevantMethod(methodToInvoke.getName()) && !Config.METHOD_BLACKLIST.contains(methodToInvoke.getName())
                    && (!Config.USE_ONLY_METHODS_FROM_FILE || (apiClassInfo != null && apiClassInfo.methodExists(methodToInvoke.getName())))) {
                if (LOGGING) {
                    Log.v(TAG, "Checking methodToInvoke: " + methodToInvoke.getName() + " class object size: " + classObjects.size() + " on " + clazz.getName());
                }
                /*if (currentRecursionDepth == 0) {
                    first_if++;
                }*/

                //boolean check = true;
                //boolean checkObj = true;

                ArrayList<Object> classObjectsAdapted = new ArrayList<>(classObjects);
                if (Modifier.isStatic(methodToInvoke.getModifiers()) && Modifier.isPublic(methodToInvoke.getModifiers())) {
                    classObjectsAdapted.add(null);
                }
                for (Object classObject : classObjectsAdapted) {
                    if (LOGGING) {
                        Log.v(TAG, "is static");
                        Log.v(TAG, "clazz.getDeclaredMethods() = " + Arrays.toString(clazz.getDeclaredMethods()));

                    }
                    Log.v(TAG, "methodToInvoke = " + methodToInvoke.toString());
                    /*if (currentRecursionDepth == 0 && checkObj) {
                        have_class_object++;
                    }
                    checkObj = false;*/
                    ArrayList<ArrayList<Object>> parameterObjectsList = createMethodParameterObjects(currentClass, methodToInvoke);

                    for (ArrayList<Object> parameterObjectsArrayList : parameterObjectsList) {
                        /*if (currentRecursionDepth == 0 && check) {
                            created_parameter_object++;
                        }
                        check = false;*/
                        Object[] parameterObjects = parameterObjectsArrayList.toArray();
                        String packageAndMethod = clazz.toString() + ":" + methodToInvoke.toString();
                        String newHierarchy = hierarchy + "||" + packageAndMethod;
                        try {
                            if (LOGGING) {
                                Log.v(TAG, "parameterObjects = " + Arrays.toString(parameterObjects) + " of method " + methodToInvoke.getName());
                            }
                            // check if methodToInvoke can be invoked
                            Object returnValue = methodToInvoke.invoke(classObject, parameterObjects);
                            if (LOGGING) {
                                Log.v(TAG, "returnValue = " + returnValue + " of method: " + methodToInvoke.getName());
                                Log.v(TAG, "currentRecursionDepth = " + currentRecursionDepth + " newHierarchy = " + newHierarchy);
                            }
                            if (returnValue != null && !Utils.isWrapperType(returnValue.getClass()) && !Utils.isCollectionType(returnValue)) {
                                if (LOGGING) {
                                    Log.v(TAG, "returnValue.getTypeName() = " + Utils.getTypeName(returnValue.getClass()) + " of method " + methodToInvoke.getName() + "() of class " + clazz.getName() + " of parentclass: "
                                            + ((parentClassObject != null) ? parentClassObject.getClass().getName() : "null") + "(method: " + ((objectCreatorMethod != null) ? objectCreatorMethod.getName() : "null") + "())");
                                }

                                if ((!Config.USE_ONLY_METHODS_FROM_FILE || methodsOfAllClassesParsed.containsKey(Utils.getTypeName(returnValue.getClass())))) {
                                    ++possiblyExploredObjectReturnMethod;
                                    try {
                                        invokableMethodsReturningObjectFileStream.write((packageAndMethod + "\n").getBytes());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }

                                if (currentRecursionDepth < Config.EXPLORATION_DEPTH_FOR_CLASS_RETURN_VALUES
                                        && (!Config.USE_ONLY_METHODS_FROM_FILE || methodsOfAllClassesParsed.containsKey(Utils.getTypeName(returnValue.getClass())))) {
                                    ArrayList<Object> classObjectsNext = new ArrayList<>();
                                    classObjectsNext.add(returnValue);
                                    if (LOGGING) {
                                        Log.v(TAG, "GOING INTO THE DEEP FOR returnValue: " + returnValue.getClass().getName());
                                    }
                                    // explore return value recursively
                                    saveInvokableMethods(returnValue.getClass(), classObjectsNext, currentRecursionDepth + 1,
                                            Utils.getTypeName(returnValue.getClass()), classObject, methodToInvoke, parameterObjects,
                                            methodsOfAllClassesParsed, newHierarchy);
                                }
                            } else {
                                APIClass apiClass = methodsOfClasses.get(clazz.getName());
                                if (apiClass == null) {
                                    apiClass = new APIClass(currentClass, classObject);
                                }

                                APIMethod apiMethod = apiClass.addMethod(contextWrapper.getApplicationContext(), classObject, methodToInvoke, parameterObjects, newHierarchy);
                                // save container class for later recreation of object
                                if (Config.CONTAINER_CLASSES.contains(currentClass)) {
                                    if (LOGGING) {
                                        Log.v(TAG, "Saving container class " + parentClassObject + " objectCreatorMethod: " + objectCreatorMethod + " objectCreatorMethodParameters: " + Arrays.toString(objectCreatorMethodParameters));
                                    }
                                    apiMethod.setParentCreators(parentClassObject, objectCreatorMethod, objectCreatorMethodParameters);
                                }

                                methodsOfClasses.put(clazz.getName(), apiClass);
                                addedMethod = true;
                                ++addedPrimitiveReturnMethods;
                                try {
                                    invokableMethodsReturningPrimitiveFileStream.write((packageAndMethod + "\n").getBytes());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        } catch (Exception e) {
                            try {
                                notInvokableMethodsFileStream.write((packageAndMethod + "\n").getBytes());
                                if (e instanceof IllegalAccessException) {
                                    illegalAccessMethodStream.write((packageAndMethod + "\n    " + (e.getCause() != null ? e.getCause().getMessage() : "getCause() returned null") + "\n").getBytes());
                                } else if (e instanceof IllegalArgumentException) {
                                    illegalArgumentMethodStream.write((packageAndMethod + "\n    " + (e.getCause() != null ? e.getCause().getMessage() : "getCause() returned null") + "\n").getBytes());
                                } else if (e instanceof InvocationTargetException) {
                                    invocationTargetMethodStream.write((packageAndMethod + "\n    " + (e.getCause() != null ? e.getCause().getMessage() : "getCause() returned null") + "\n").getBytes());
                                } else if (e instanceof NullPointerException) {
                                    nullPointerMethodStream.write((packageAndMethod + "\n    " + (e.getCause() != null ? e.getCause().getMessage() : "getCause() returned null") + "\n").getBytes());
                                }
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                            ++notInvokableMethods;
                            e.printStackTrace();
                        }
                        ++allMethods;
                    }
                }
            }
            Log.v(TAG, (addedMethod ? "[+?] Added (for now)" : "[-] Removed") + " method " + methodToInvoke.getName());
        }
    }

    private void parameterObjectPermutations(ArrayList<ArrayList<Object>> parameterObjects, ArrayList<Object> permutation, ArrayList<ArrayList<Object>> result, int position) {
        if (position < parameterObjects.size()) {
            for (int i = 0; i < parameterObjects.get(position).size(); ++i) {
                permutation.add(parameterObjects.get(position).get(i));
                parameterObjectPermutations(parameterObjects, permutation, result, position + 1);
                permutation.remove(permutation.size() - 1);
            }
        } else {
            result.add(new ArrayList<>(permutation));
        }
    }

    @NonNull
    private ArrayList<ArrayList<Object>> createMethodParameterObjects(String classOfMethod, Method method) {
        ArrayList<String[]> allParameterNames = new ArrayList<>();
        if (LOGGING) {
            Log.v(TAG, "trying to get params. class: " + classOfMethod +
                    " method.getName() = " + method.getName() + " private: " + Modifier.isPrivate(method.getModifiers()) +
                    " protected: " + Modifier.isProtected(method.getModifiers()) + " public: " + Modifier.isPublic(method.getModifiers()));
        }

        APIClassInfo apiClassInfo = methodsOfAllClassesParsed.getOrDefault(classOfMethod, null);
        if (apiClassInfo != null && apiClassInfo.methodExists(method.getName())) {
            allParameterNames = apiClassInfo.getMethodInfo(method.getName()).getParameterNamesWithLength(method.getParameterTypes().length);
        }

        ArrayList<ArrayList<Object>> parameterObjects = new ArrayList<>();
        for (int paramCounter = 0; paramCounter < method.getParameterTypes().length; ++paramCounter) {
            parameterObjects.add(new ArrayList<>());
        }

        for (int nameCounter = 0; nameCounter < allParameterNames.size(); ++nameCounter) {
            String[] parameterNames = allParameterNames.get(nameCounter);
            for (int paramCounter = 0; paramCounter < method.getParameterTypes().length; ++paramCounter) {
                Object predefinedValue = predefines.getPredefinedParameterValue(classOfMethod, method.getName(), parameterNames[paramCounter]);

                if (predefinedValue instanceof LinkedHashMap) {
                    LinkedHashMap<String, Object> predefinedObject = (LinkedHashMap<String, Object>) predefinedValue;
                    if ((boolean) predefinedObject.get("parameter_is_array")) {
                        predefinedValue = predefinedObject.get("value");
                        if (Utils.isAssignable(predefinedValue.getClass(), method.getParameterTypes()[paramCounter], true)) {
                            parameterObjects.get(paramCounter).add(predefinedValue);
                            Log.v(TAG, "ADDED ARRAY predefinedObject = " + predefinedObject);
                        }
                    } else {
                        ArrayList<Object> predefinedValues = ((ArrayList<Object>) predefinedObject.get("value"));
                        for (Object predefinedValue_ : predefinedValues) {
                            if (Utils.isAssignable(predefinedValue_.getClass(), method.getParameterTypes()[paramCounter], true)) {
                                parameterObjects.get(paramCounter).add(predefinedValue_);
                                Log.v(TAG, "ADDED NON ARRAY predefinedObject = " + predefinedObject);
                            }
                        }
                    }
                } else if (predefinedValue != null && Utils.isAssignable(predefinedValue.getClass(), method.getParameterTypes()[paramCounter], true)) {
                    parameterObjects.get(paramCounter).add(predefinedValue);
                }
            }
        }

        for (int paramCounter = 0; paramCounter < method.getParameterTypes().length; ++paramCounter) {
            if (parameterObjects.get(paramCounter).isEmpty()) {
                Object defaultParameter = Utils.getDefaultParameter(method.getParameterTypes()[paramCounter]);
                if (defaultParameter != null) {
                    parameterObjects.get(paramCounter).add(defaultParameter);
                }
            }
            if (parameterObjects.get(paramCounter).isEmpty()) {
                Constructor<?>[] constructors = method.getParameterTypes()[paramCounter].getConstructors();
                try {
                    ArrayList<Object> classObjects = createClassObject(constructors, parser.getConstructorsOfAllClassesParsed());
                    for (Object classObject : classObjects) {
                        if (classObject != null) {
                            parameterObjects.get(paramCounter).add(classObject);
                        }
                    }
                } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                    try {
                        parameterCreationExceptionStream.write((Arrays.toString(constructors) + "\n  " + e.getClass().getName() + "\n    " + (e.getCause() != null ? e.getCause().getMessage() : "getCause() returned null") + "\n").getBytes());
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    e.printStackTrace();
                }
            }
        }

        ArrayList<ArrayList<Object>> result = new ArrayList<>();
        parameterObjectPermutations(parameterObjects, new ArrayList<>(), result, 0);
        return result;
    }

    private ArrayList<Object> createClassObject(Constructor[] constructors, HashMap<String, ArrayList<String[]>> constructorsAndParameters) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        ArrayList<Object> constructorObjects = new ArrayList<>();

        constructorLoop:
        for (Constructor constructor : constructors) {
            ArrayList<ArrayList<Object>> parameterObjects = new ArrayList<>();
            String[] parameterNames = getConstructorParameterNames(constructorsAndParameters, constructor);

            for (int paramCounter = 0; paramCounter < constructor.getParameterTypes().length; ++paramCounter) {
                Class param = constructor.getParameterTypes()[paramCounter];
                ArrayList<Object> classObjects = new ArrayList<>();

                Object predefinedValue = null;
                if (parameterNames != null) {
                    String[] splittedConstructorName = constructor.getName().split("\\.");
                    predefinedValue = predefines.getPredefinedParameterValue(constructor.getName(), splittedConstructorName[splittedConstructorName.length - 1], parameterNames[paramCounter]);
                }

                if (predefinedValue != null & param.isInstance(predefinedValue)) {
                    classObjects.add(predefinedValue);
                } else if (Utils.getDefaultParameter(param) != null) {
                    classObjects.add(Utils.getDefaultParameter(param));
                } else if (Modifier.isAbstract(param.getModifiers()) || param.getName().equals(constructor.getName()) ||
                        (param.getName().equals("android.os.Bundle") && constructor.getName().equals("android.os.PersistableBundle"))
                        || (param.getName().equals("android.telecom.AudioState") && constructor.getName().equals("android.telecom.CallAudioState"))
                        || (param.getName().equals("java.io.PipedInputStream") && constructor.getName().equals("java.io.PipedOutputStream"))
                        || (param.getName().equals("java.io.PipedReader") && constructor.getName().equals("java.io.PipedWriter"))) {
                    continue constructorLoop;
                } else if (param.isEnum()) {
                    classObjects = new ArrayList<>(Arrays.asList(param.getEnumConstants()));
                } else {
                    classObjects = createClassObject(param.getConstructors(), constructorsAndParameters);
                    if (classObjects.size() < 1) {
                        continue constructorLoop;
                    }
                }

                parameterObjects.add(new ArrayList<>());
                parameterObjects.get(paramCounter).addAll(classObjects);
            }

            if (LOGGING) {
                Log.v(TAG, "constructor.getParameterTypes() = " + Arrays.toString(constructor.getParameterTypes()));
                if (!constructor.getName().equals("android.hardware.usb.UsbDeviceConnection") && !constructor.getName().equals("android.app.WallpaperInfo") && !constructor.getName().equals("android.mtp.MtpDevice")
                        && !constructor.getName().equals("android.view.inputmethod.InputMethodInfo")) {
                    Log.v(TAG, "created parameterObjects = " + parameterObjects + " for constructor " + (!constructor.getName().equals("android.hardware.usb.UsbDeviceConnection") ? constructor.toString() : constructor.getName()));
                }
            }

            if (!constructor.getName().equals("java.net.Socket") && !constructor.getName().equals("java.math.BigInteger") &&
                    !constructor.getName().equals("android.media.FaceDetector") && !constructor.getName().equals("android.webkit.WebView")) {
                ArrayList<ArrayList<Object>> result = new ArrayList<>();
                parameterObjectPermutations(parameterObjects, new ArrayList<>(), result, 0);
                for (ArrayList<Object> parameterObject : result) {
                    try {
                        constructorObjects.add(constructor.newInstance(parameterObject.toArray()));
                    } catch (Exception e) {
                        exceptionWhileCreation++;
                        try {
                            exceptionWhileCreationStream.write((constructor.toString() + "\n").getBytes());
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        e.printStackTrace();
                    }
                }
            } else {
                //constructorObjects.add(null);
            }
        }
        return constructorObjects;
    }

    @Nullable
    private String[] getConstructorParameterNames(HashMap<String, ArrayList<String[]>> constructorsAndParameters, Constructor constructor) {
        ArrayList<String[]> parameterNames = constructorsAndParameters.get(constructor.getName());
        if (parameterNames != null) {
            parameterNames.forEach(strings -> Log.v(TAG, "parameterNames all: " + Arrays.toString(strings)));
            for (String[] possibleParameters : parameterNames) {
                if (possibleParameters.length == constructor.getParameterTypes().length) {
                    System.arraycopy(possibleParameters, 0, possibleParameters, 0, possibleParameters.length);
                    return possibleParameters;
                }
            }
        }
        return null;
    }

    // loads parsed classes, creates object and explores the methods by calling the saveInvokableMethods method
    private void loadParsedClasses() throws IOException {
        HashMap<String, ArrayList<String[]>> constructorsAndParameters = parser.getConstructorsOfClassesToCheckParsed();
        int notFoundClasses = 0, errorClasses = 0, totalClasses = 0;

        Log.v(TAG, "Loading the following constructorsAndParameters: " + Collections.singletonList(constructorsAndParameters));
        if (LOGGING) {
            FileLogging.logClassesToLoad(methodsOfAllClassesParsed);
        }

        for (Map.Entry<String, APIClassInfo> parsedMethodsOfClass : methodsOfClassesToCheckParsed.entrySet()) {
            String currentClass = parsedMethodsOfClass.getKey();

            Class<?> clazz = null;
            ArrayList<Object> classObjects = new ArrayList<>();
            try {
                ++totalClasses;
                clazz = Class.forName(currentClass);
                Log.v(TAG, " Loading class: " + clazz.getName());

                if (Modifier.isAbstract(clazz.getModifiers()) || Modifier.isInterface(clazz.getModifiers())) {
                    try {
                        abstractOrInterfacesStream.write((currentClass + "\n").getBytes());
                        int missedMethodsDueToAbstractLocal = 0;
                        StringBuilder methodsMissedDueToAbstractOrInterface = new StringBuilder();
                        for (Map.Entry<String, APIMethodInfo> method : parsedMethodsOfClass.getValue().methodInfos.entrySet()) {
                            if (Config.isRelevantMethod(method.getKey())) {
                                missedMethodsDueToAbstractLocal += method.getValue().getParameterNames().size();
                                methodsMissedDueToAbstractOrInterface.append(method.getKey()).append(": ");
                                for (String[] paramName : method.getValue().getParameterNames()) {
                                    methodsMissedDueToAbstractOrInterface.append(Arrays.toString(paramName)).append(", ");
                                }
                                methodsMissedDueToAbstractOrInterface.append(" || ");
                            }
                        }
                        missedMethodsDueAbstractInterface += missedMethodsDueToAbstractLocal;
                        abstractOrInterfacesMethodsStream.write((currentClass + "\n  missedMethodsDueToNotFound: "
                                + missedMethodsDueToAbstractLocal + "\n  " + methodsMissedDueToAbstractOrInterface + "\n").getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    continue;
                }
                Constructor<?>[] constructors = clazz.getConstructors();
                classObjects = createClassObject(constructors, constructorsAndParameters);
            } catch (Exception e) {
                if (e instanceof ClassNotFoundException) {
                    ++notFoundClasses;
                    try {
                        notFoundClassesStream.write((currentClass + "\n").getBytes());
                        int missedMethodsDueToNotFoundLocal = 0;
                        StringBuilder methodsHere = new StringBuilder();
                        for (Map.Entry<String, APIMethodInfo> method : parsedMethodsOfClass.getValue().methodInfos.entrySet()) {
                            if (Config.isRelevantMethod(method.getKey())) {
                                missedMethodsDueToNotFoundLocal += method.getValue().getParameterNames().size();
                                methodsHere.append(method.getKey()).append(": ");
                                for (String[] paramName : method.getValue().getParameterNames()) {
                                    methodsHere.append(Arrays.toString(paramName)).append(", ");
                                }
                                methodsHere.append(" || ");
                                missedMethodsDueToNotFoundSingle++;
                            }
                        }
                        missedMethodsDueToNotFound += missedMethodsDueToNotFoundLocal;
                        notFoundClassesMoreInformationStream.write((currentClass + "\n  missedMethodsDueToNotFound: " + missedMethodsDueToNotFoundLocal + "\n  " + methodsHere + "\n").getBytes());
                        continue;
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                } else {
                    ++errorClasses;
                    try {
                        errorClassesStream.write((currentClass + "\n").getBytes());
                        int missedMethodsDueToClassCreationErrorThisClass = 0;
                        for (Map.Entry<String, APIMethodInfo> method : parsedMethodsOfClass.getValue().methodInfos.entrySet()) {
                            if (Config.isRelevantMethod(method.getKey())) {
                                missedMethodsDueToClassCreationErrorThisClass += method.getValue().getParameterNames().size();
                                missedMethodsDueToClassCreationErrorSingle++;
                            }
                        }
                        missedMethodsDueToClassCreationError += missedMethodsDueToClassCreationErrorThisClass;
                        classCreationExceptionStream.write((currentClass + "\n  " + e.getClass().getName() + "\n  "
                                + (e.getCause() != null ? e.getCause().getMessage() : "getCause() returned null") + "\n    relevant methods lost: " + missedMethodsDueToClassCreationErrorThisClass + "\n").getBytes());
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                e.printStackTrace();
            }
            if (clazz == null) {
                throw new RuntimeException("No class found");
                //continue;
            }

            if (classObjects.size() == 0) {
                try {
                    for (Map.Entry<String, APIMethodInfo> method : parsedMethodsOfClass.getValue().methodInfos.entrySet()) {
                        if (Config.isRelevantMethod(method.getKey())) {
                            boolean first = true;
                            int methodsCounted = 0;
                            int methodsSaved = 0;
                            for (Method realMethod : clazz.getDeclaredMethods()) {
                                if (realMethod.getName().equals(method.getKey()) && !Modifier.isStatic(realMethod.getModifiers()) && Modifier.isPublic(realMethod.getModifiers())) {
                                    methodsCounted += 1;
                                    if (first) {
                                        methodsSaved += method.getValue().getParameterNames().size();
                                        first = false;
                                    }
                                    missedMethodsDueToNoObjectStream.write((realMethod.toString() + "\n").getBytes());
                                }
                            }
                            missedMethodsDueToNoObject += methodsCounted;
                            missedMethodsDueToNoObjectParam += methodsSaved;
                            if (methodsCounted != methodsSaved) {
                                missedMethodsDueToNoObjectStream.write(("  differs; methodsCounted: " + methodsCounted + " methodsSaved" + methodsSaved + "\n").getBytes());
                            }
                        }
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }

            int methods = 0;
            for (Map.Entry<String, APIMethodInfo> method : parsedMethodsOfClass.getValue().methodInfos.entrySet()) {
                if (Config.isRelevantMethod(method.getKey()))
                    methods += method.getValue().getParameterNames().size();
            }
            methodsWhichWeCreatedObjectFor += methods;
            try {
                checkMethodStream.write((currentClass + " PARSED: " + Integer.toString(methods) + "\n").getBytes());
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            //calledFromHere = true;
            if (classObjects.size() == 0) {
                noClassObject.write((clazz.toString() + "\n").getBytes());
                noClassObject.write(("  " + clazz.getConstructors().length + "  " + Arrays.toString(clazz.getConstructors()) + "\n").getBytes());
            }
            saveInvokableMethods(clazz, classObjects, 0, Utils.getTypeName(clazz), null, null, null, methodsOfClassesToCheckParsed, "");
        }

        Log.i(TAG, "notFoundClasses = " + notFoundClasses + ", error classes = " + errorClasses + " of " + totalClasses);
    }

    private void loadPredefinedClasses(ContextWrapper contextWrapper) {
        HashMap<String, Object> classObjects = Predefines.loadCustomObjects(contextWrapper);
        for (Map.Entry<String, Object> classObject : classObjects.entrySet()) {
            ArrayList<Object> objectList = new ArrayList<>();
            String className = classObject.getKey();
            try {
                Class clazz = Class.forName(className);
                Object castedObject = clazz.cast(classObject.getValue());
                objectList.add(castedObject);
                saveInvokableMethods(clazz, objectList, 0, className, null, null, null, methodsOfClassesToCheckParsed, "");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                throw new RuntimeException(Config.LOGCAT_RUNTIME_ERROR + "Predefined class not found!");
            }
        }
    }
}
