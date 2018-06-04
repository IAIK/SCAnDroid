package at.tugraz.iaik.scandroid;

import android.content.ContextWrapper;
import android.util.Log;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by Gerald Palfinger on 27.11.17.
 */

public class Config {
    private static final String TAG = Config.class.getName();

    static boolean USE_ONLY_METHODS_FROM_FILE;
    static int EXPLORATION_DEPTH_FOR_CLASS_RETURN_VALUES;
    public static int NR_OF_INIT_LOOPS;
    private static ArrayList<String> METHOD_PREFIXES;
    static ArrayList<String> METHOD_BLACKLIST;
    static ArrayList<String> CONTAINER_CLASSES;
    public static boolean USE_STRING_STREAM;
    public static boolean HAR_STYLE_LOG_FILES;
    public static boolean FILTER_SAME_VALUES;
    public static boolean FILTER_DOUBLE_METHODS;

    static final int methods_file_resource = R.raw.leaking_methods;
    static final int constructors_file_resource = R.raw.leaking_constructors;

    // used for return value exploration
    static final int all_methods_file_resource = R.raw.methods_without_graphics_v27_fix3_wrong_class_all;
    static final int all_constructors_file_resource = R.raw.constructors_without_graphics_v27_fix3_wrong_class_all;

    public static final int methods_for_methods_dumper = R.raw.methods_without_graphics_v27_fix3_wrong_class_all;

    private static final String LOGCAT_SYNC_MESSAGE = "--SYNC_MESSAGE--:";
    public static final String LOGCAT_LOADING_DONE = LOGCAT_SYNC_MESSAGE + "LOADING_DONE";
    public static final String LOGCAT_INIT_DONE = LOGCAT_SYNC_MESSAGE + "INIT_DONE";
    public static final String LOGCAT_RUNTIME_ERROR = LOGCAT_SYNC_MESSAGE + "RUNTIME_ERROR: ";
    public static final String LOGCAT_FILEPATH = LOGCAT_SYNC_MESSAGE + "HARVESTING_DONE_RESULTS_FILEPATH:";

    public static void loadConfig(ContextWrapper contextWrapper) {
        InputStream input = contextWrapper.getResources().openRawResource(R.raw.config);
        Yaml yaml = new Yaml();
        LinkedHashMap<String, Object> config = (LinkedHashMap<String, Object>) ((LinkedHashMap<String, Object>) yaml.load(input)).get("config");
        USE_ONLY_METHODS_FROM_FILE = (boolean) config.get("USE_ONLY_METHODS_FROM_FILE");
        EXPLORATION_DEPTH_FOR_CLASS_RETURN_VALUES = (int) config.get("EXPLORATION_DEPTH_FOR_CLASS_RETURN_VALUES");
        METHOD_PREFIXES = (ArrayList<String>) config.get("METHOD_PREFIXES");
        METHOD_BLACKLIST = (ArrayList<String>) config.get("METHOD_BLACKLIST");
        NR_OF_INIT_LOOPS = (int) config.get("NR_OF_INIT_LOOPS");
        USE_STRING_STREAM = (boolean) config.get("USE_STRING_STREAM");
        HAR_STYLE_LOG_FILES = (boolean) config.get("HAR_STYLE_LOG_FILES");
        FILTER_SAME_VALUES = (boolean) config.get("FILTER_SAME_VALUES");
        FILTER_DOUBLE_METHODS = (boolean) config.get("FILTER_DOUBLE_METHODS");
        loadContainerClasses(contextWrapper);
    }

    private static void loadContainerClasses(ContextWrapper contextWrapper) {
        InputStream input = contextWrapper.getResources().openRawResource(R.raw.container_classes);
        Yaml yaml = new Yaml();
        CONTAINER_CLASSES = ((LinkedHashMap<String, ArrayList<String>>) yaml.load(input)).get("container_classes");
        Log.v(TAG, "CONTAINER_CLASSES = " + CONTAINER_CLASSES);
    }

    public static boolean isRelevantMethod(String methodName) {
        for (String methodPrefix : METHOD_PREFIXES) {
            if (methodName.toLowerCase().startsWith(methodPrefix.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
