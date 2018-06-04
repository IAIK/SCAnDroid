package at.tugraz.iaik.scandroid;

import android.util.Log;

import java.util.Arrays;
import java.util.Map;

import at.tugraz.iaik.scandroid.parsedTypes.APIClassInfo;
import at.tugraz.iaik.scandroid.parsedTypes.APIMethodInfo;
import at.tugraz.iaik.scandroid.types.CustomHashMap;

/**
 * Created by Gerald Palfinger on 10.03.18.
 */

class FileLogging {
    private static final String TAG = FileLogging.class.getName();

    static void logClassesToLoad(CustomHashMap<String, APIClassInfo> methodsOfAllClassesParsed) {
        int methodsParsed = 0;
        for (Map.Entry<String, APIClassInfo> parsedMethodsOfClass : methodsOfAllClassesParsed.entrySet()) {
            int missedMethodsDueToAbstractLocal = 0;
            StringBuilder methodsMissedDueToAbstractOrInterface = new StringBuilder();
            for (Map.Entry<String, APIMethodInfo> method : parsedMethodsOfClass.getValue().methodInfos.entrySet()) {
                if (Config.isRelevantMethod(method.getKey())) {
                    missedMethodsDueToAbstractLocal += method.getValue().getParameterNames().size();
                    if(method.getValue().getParameterNames().size() == 0)
                        throw new RuntimeException("is zero!");
                    methodsMissedDueToAbstractOrInterface.append(method.getKey()).append(": ");
                    for (String[] paramName : method.getValue().getParameterNames()) {
                        methodsMissedDueToAbstractOrInterface.append(Arrays.toString(paramName)).append(", ");
                    }
                    methodsMissedDueToAbstractOrInterface.append(" || ");
                }
            }
            methodsParsed += missedMethodsDueToAbstractLocal;
            Log.v(TAG, "methodsParsed = " + methodsParsed);
        }
    }
}
