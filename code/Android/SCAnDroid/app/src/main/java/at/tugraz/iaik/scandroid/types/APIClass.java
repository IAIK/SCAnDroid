package at.tugraz.iaik.scandroid.types;

import android.content.Context;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by Gerald Palfinger on 14.09.17.
 */

public class APIClass {
    private final Object classObject;
    private final String className;

    private final CustomHashMap<String, ArrayList<APIMethod>> methodsMap = new CustomHashMap<>();

    public APIClass(String className, Object classObject) {
        this.className = className;
        this.classObject = classObject;
    }

    public APIMethod addMethod(Context context, Object classObject, Method method, Object[] parameterObjects, String hierarchy) {
        ArrayList<APIMethod> methods = methodsMap.getOrDefault(method.getName(), new ArrayList<>());
        APIMethod apiMethod = new APIMethod(context, classObject, method, parameterObjects, className, hierarchy);
        methods.add(apiMethod);
        methodsMap.put(method.getName(), methods);
        return apiMethod;
    }

    @Override
    public String toString() {
        return "APIClass{" +
                "classObject=" + classObject +
                ", className='" + className + '\'' +
                ", methodsMap=" + methodsMap +
                '}';
    }

    public ArrayList<APIMethod> getAllMethods() {
        ArrayList<APIMethod> apiMethods = new ArrayList<>();
        for (Map.Entry<String, ArrayList<APIMethod>> methods : methodsMap.entrySet()) {
            apiMethods.addAll(methods.getValue());
        }
        return apiMethods;
    }

    public boolean removeAPIMethod(APIMethod apiMethodToRemove) {
        boolean removed = false;
        for (ArrayList<APIMethod> apiMethods : methodsMap.values()) {
            if (apiMethods.remove(apiMethodToRemove)) {
                removed = true;
            }
        }
        return removed;
    }
}