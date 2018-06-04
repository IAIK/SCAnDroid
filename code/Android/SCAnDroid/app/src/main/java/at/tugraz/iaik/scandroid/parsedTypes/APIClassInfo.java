package at.tugraz.iaik.scandroid.parsedTypes;

import android.util.Log;

import at.tugraz.iaik.scandroid.types.CustomHashMap;

/**
 * Created by Gerald Palfinger on 31.10.17.
 */

public class APIClassInfo {
    private static final String TAG = APIClassInfo.class.getName();
    private final String className;
    public final CustomHashMap<String, APIMethodInfo> methodInfos = new CustomHashMap<>();

    public APIClassInfo(String className) {
        this.className = className;
    }

    public void addMethod(String methodName) {
        APIMethodInfo methodInfo = methodInfos.getOrDefault(methodName, new APIMethodInfo(methodName));
        methodInfo.addNewSignature(new String[0]);
        methodInfos.put(methodName, methodInfo);
    }

    public void addMethod(String methodName, String[] parameterNames) {
        APIMethodInfo methodInfo = methodInfos.getOrDefault(methodName, new APIMethodInfo(methodName));
        methodInfo.addNewSignature(parameterNames);
        methodInfos.put(methodName, methodInfo);
    }

    public boolean methodExists(String name) {
        boolean exists = methodInfos.containsKey(name);
        if(!exists) {
            Log.i(TAG, name + " does not exist in parsed file");
            Log.v(TAG, "methodInfos = " + methodInfos);
        }
        return exists;
    }

    @Override
    public String toString() {
        return className + ". methods: " + methodInfos.toString();
    }

    public APIMethodInfo getMethodInfo(String name) {
        return methodInfos.getOrDefault(name, null);
    }

    public String getClassName() {
        return className;
    }
}
