package at.tugraz.iaik.scandroid.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import at.tugraz.iaik.scandroid.types.APIClass;
import at.tugraz.iaik.scandroid.APIClassLoader;
import at.tugraz.iaik.scandroid.types.APIMethod;
import at.tugraz.iaik.scandroid.Config;
import at.tugraz.iaik.scandroid.Parser;
import at.tugraz.iaik.scandroid.Predefines;
import at.tugraz.iaik.scandroid.Utils;
import at.tugraz.iaik.scandroid.types.CustomHashMap;
import dalvik.system.DexFile;

public class APIHarvestingService extends IntentService {
    private static final String TAG = APIHarvestingService.class.getName();
    private static final boolean DEBUG = false;

    private volatile boolean collectLog = false;
    private volatile String targetName = null;
    private String currentTargetName = null;
    private static APIHarvestingService instance;
    private int mainLoopCounter = 0;

    private FileOutputStream labelsFileStream;
    private FileOutputStream addedMethodsStream;
    private FileOutputStream removedMethodsStream;

    public static APIHarvestingService getInstance() {
        return instance;
    }

    public APIHarvestingService() {
        super("APIHarvestingService");
    }

    // Sets new target name received by the Backend
    public void setNewTargetName(String targetName) {
        if (mainLoopCounter < Config.NR_OF_INIT_LOOPS) {
            throw new RuntimeException(Config.LOGCAT_RUNTIME_ERROR + "Init not done yet! Did something go wrong?");
        }
        this.targetName = targetName;
    }

    public static void startAPIHarvestingService(Context context) {
        Intent intent = new Intent(context, APIHarvestingService.class);
        context.startService(intent);
    }

    @Override
    public void onDestroy() {
        instance = null;
        super.onDestroy();
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        instance = this;

        // Load config
        Config.loadConfig(this);

        cleanupFileDir();

        initDebugFiles();

        startLogging();
    }

    // Deletes old record files
    private void cleanupFileDir() {
        File externalFilesDir = getExternalFilesDir(null);
        assert externalFilesDir != null;
        for (File file : externalFilesDir.listFiles()) {
            if (file.isFile()) {
                file.delete();
            }
        }
    }

    private void startLogging() {
        collectLog = true;

        // Setup: "parse" parsed files, load predefines, try to invoke each method (recursion flattening)
        CustomHashMap<String, APIClass> methodsOfClasses;
        try {
            Parser parser = new Parser(this);
            Predefines predefines = new Predefines(this);
            APIClassLoader classLoader = new APIClassLoader(this, predefines, parser);
            methodsOfClasses = classLoader.getMethodsOfClasses();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(Config.LOGCAT_RUNTIME_ERROR + "Init failed!");
        }

        Log.i("APIHS", Config.LOGCAT_LOADING_DONE);

        // Main loop
        while (collectLog || targetName != null) {
            mainLoop(methodsOfClasses);
            if (mainLoopCounter == Config.NR_OF_INIT_LOOPS ||
                    (mainLoopCounter >= Config.NR_OF_INIT_LOOPS && targetName == null && currentTargetName == null)) {
                APIMethod.KEEP_MEASUREMENTS = true;
                while (targetName == null) {
                    Log.i("APIHS", Config.LOGCAT_INIT_DONE);
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void initDebugFiles() {
        File addedMethodsFile = new File(getApplicationContext().getExternalFilesDir(null), "added_methods.info");
        File removedMethodsFile = new File(getApplicationContext().getExternalFilesDir(null), "removed_methods.info");
        try {
            addedMethodsStream = new FileOutputStream(addedMethodsFile);
            removedMethodsStream = new FileOutputStream(removedMethodsFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void stopLogging() {
        targetName = "";
        collectLog = false;
        Log.i(TAG, "--- STOPPED LOGGING ---");
    }

    private ArrayList<String> addedMethods = new ArrayList<>();

    private void mainLoop(CustomHashMap<String, APIClass> methodsOfClasses) {
        boolean removeUnchangedMethods = false, measurementDone = false;

        ++mainLoopCounter;

        if (mainLoopCounter < Config.NR_OF_INIT_LOOPS) {
            Log.i(TAG, "[INIT] Loop number: " + mainLoopCounter + " of " + Config.NR_OF_INIT_LOOPS);
        } else if (targetName != null && currentTargetName == null) {
            Log.i(TAG, "----------------- REMOVING UNCHANGED METHODS -----------------");
            removeUnchangedMethods = true;
        } else if (targetName != null && currentTargetName != null) {
            Log.i(TAG, "----------------- MEASUREMENT DONE, FINISHING OLD LABEL -----------------");
            measurementDone = true;
        }

        if (Config.HAR_STYLE_LOG_FILES && measurementDone) {
            writeLabelToLabelFile(currentTargetName + "\n");
        }

        for (Map.Entry<String, APIClass> mapEntry : methodsOfClasses.entrySet()) {
            APIClass apiClass = mapEntry.getValue();
            ArrayList<APIMethod> methods = apiClass.getAllMethods();

            for (APIMethod apiMethod : methods) {
                if (removeUnchangedMethods) {
                    if (!apiMethod.stringStreamChangedMoreThanOnce() || (Config.FILTER_DOUBLE_METHODS && addedMethods.contains(apiMethod.toString()))) {
                        Log.i(TAG, "[-] Removed " + apiMethod.toString());
                        apiClass.removeAPIMethod(apiMethod);
                        try {
                            removedMethodsStream.write((apiMethod.toString() + "\n").getBytes());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else if (!Config.FILTER_DOUBLE_METHODS || !addedMethods.contains(apiMethod.toString())) {
                        Log.i(TAG, "[+] Added " + apiMethod.toString());
                        try {
                            addedMethodsStream.write((apiMethod.toString() + "\n").getBytes());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        addedMethods.add(apiMethod.toString());
                        apiMethod.firstTimestamp = 0;
                    }
                } else if (measurementDone) {
                    apiMethod.addTargetToStringStream(currentTargetName);
                    apiMethod.writeToFile();
                    apiMethod.firstTimestamp = 0;
                } else {
                    pollGetters(apiMethod);
                }
            }
        }

        if (removeUnchangedMethods || measurementDone) {
            currentTargetName = targetName;
            targetName = null;
            Log.i(TAG, "------------------- CHANGING LABEL TO : " + currentTargetName + " -------------------");
        }
    }

    // only used for HAR style log files
    private void writeLabelToLabelFile(String label) {
        try {
            if (labelsFileStream == null) {
                File labelsFile = new File(getExternalFilesDir(null), "labels.txt");
                labelsFileStream = new FileOutputStream(labelsFile);
            }
            labelsFileStream.write(label.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void pollGetters(APIMethod apiMethod) {
        try {
            long currentTime = System.currentTimeMillis();
            if (apiMethod.firstTimestamp == 0) {
                apiMethod.firstTimestamp = currentTime;
            }
            // Calculate time stamp
            long time = currentTime - apiMethod.firstTimestamp + 1;

            // invoke method
            Object returnValue = apiMethod.getMethod().invoke(apiMethod.getClassObject(), apiMethod.getParameterObjects());

            // save String representation of return value
            String fileContent;
            if (returnValue == null) {
                fileContent = "null";
            } else if (returnValue.getClass().isArray()) {
                fileContent = Arrays.deepToString(Utils.convertToObjectArray(returnValue));
            } else if (returnValue instanceof Collection<?>) {
                fileContent = Arrays.deepToString(((Collection) returnValue).toArray());
            } else if (returnValue instanceof Map<?, ?>) {
                fileContent = returnValue.toString();
            } else if (returnValue.getClass().equals(String.class)) {
                fileContent = (String) returnValue;
            } else {
                fileContent = returnValue.toString();
            }

            if (!fileContent.equals(apiMethod.previouslyWrittenToFile) || (!Config.FILTER_SAME_VALUES && APIMethod.KEEP_MEASUREMENTS)) {
                apiMethod.previouslyWrittenToFile = fileContent;
                apiMethod.addMeasurement(fileContent, Long.toString(time));
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        } catch (Exception e) {
            Log.e(TAG, "other exception occurred");
            e.printStackTrace();
        }
    }

    // --------------------------------------------- OLD ATTEMPTS ----------------------------------

    // API 26+ (Android O+) only
    @Nullable
    private static List<String> getParameterNames(Method method) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            java.lang.reflect.Parameter[] parameters = method.getParameters();
            List<String> parameterNames = new ArrayList<>();

            for (java.lang.reflect.Parameter parameter : parameters) {
                if (!parameter.isNamePresent()) {
                    Log.i(TAG, "PARAMETERNAME unknown");
                    parameterNames.add("UNKNOWN");
                    continue;
                }
                parameterNames.add(parameter.getName());
            }
            return parameterNames;
        }
        return null;
    }

    //Only returns app_usage used by this app
    public Set<Class<?>> getClasspathClasses(Context context) throws ClassNotFoundException, IOException {
        Set<Class<?>> classes = new HashSet<>();

        Log.v(TAG, "context.getApplicationInfo().sourceDir = " + context.getApplicationInfo().sourceDir);
        Log.v(TAG, "context.getApplicationInfo().className = " + context.getApplicationInfo().className);
        Log.v(TAG, "context.getApplicationInfo().publicSourceDir = " + context.getApplicationInfo().publicSourceDir);
        Log.v(TAG, "context.getApplicationInfo().nativeLibraryDir = " + context.getApplicationInfo().nativeLibraryDir);

        DexFile dex = new DexFile(context.getApplicationInfo().sourceDir);
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();//Context.class.getClassLoader();
        Enumeration<String> entries = dex.entries();
        while (entries.hasMoreElements()) {
            String entry = entries.nextElement();
            classes.add(classLoader.loadClass(entry));
        }
        return classes;
    }

    //not working
    private void loadClassesWithReflection() {
                /*List<APIClassLoader> classLoadersList = new LinkedList<APIClassLoader>();
        classLoadersList.add(ClasspathHelper.contextClassLoader());
        classLoadersList.add(ClasspathHelper.staticClassLoader());

        Reflections reflections = new Reflections("android", new SubTypesScanner(false));

        APIClassLoader loader = this.getClass().getClassLoader();
        ClassPath p = null;
        try {
            p = ClassPath.from(loader);
            ImmutableSet<ClassPath.ClassInfo> list = p.getTopLevelClasses();
            System.out.println("list = " + list);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Set<Class<?>> app_usage = reflections.getSubTypesOf(Object.class);
        System.out.println("app_usage = " + app_usage);*/

        /*try {
            //app_usage = getClasspathClasses(getApplicationContext(), "");
            System.out.println("app_usage = " + app_usage);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }*/


        /*APIClassLoader loader = this.getClass().getClassLoader();
        ClassPath p = null;
        try {
            p = ClassPath.from(loader);
            ImmutableSet<ClassPath.ClassInfo> list = p.getTopLevelClasses();
            System.out.println("list = " + list);
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }
}