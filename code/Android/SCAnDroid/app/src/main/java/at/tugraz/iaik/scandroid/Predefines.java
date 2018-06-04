package at.tugraz.iaik.scandroid;

import android.content.ContextWrapper;
import android.support.annotation.Nullable;
import android.util.Log;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import bsh.EvalError;
import bsh.Interpreter;

/**
 * Created by Gerald Palfinger on 27.11.17.
 */

public class Predefines {
    private final String TAG = Predefines.class.getName();

    private LinkedHashMap<String, Object> predefinedParameterValues;
    private final ContextWrapper contextWrapper;

    public Predefines(ContextWrapper contextWrapper) {
        this.contextWrapper = contextWrapper;
        loadParameterMap(contextWrapper);
    }

    private void setPredefinedVariables(Interpreter interpreter) throws EvalError {
        interpreter.set("context", contextWrapper);
    }

    private static HashMap<String, Object> loadCustomParameterObjects(ContextWrapper contextWrapper) {
        HashMap<String, Object> customParameterObjects = new HashMap<>();
        // Add custom parameter objects for methods and constructors here - usually not needed anymore as parameterValues.yaml file is now sophisticated enough
        //customParameterObjects.put("context", contextWrapper.getBaseContext());
        return customParameterObjects;
    }

    static HashMap<String, Object> loadCustomObjects(ContextWrapper contextWrapper) {
        HashMap<String, Object> customObjects = new HashMap<>();
        // Add custom objects to call methods on here
        customObjects.put("android.content.Context", contextWrapper.getApplicationContext());
        return customObjects;
    }

    private Object interpretStatement(Object statement) {
        Interpreter interpreter = new Interpreter();
        try {
            setPredefinedVariables(interpreter);

            String strStatement = (String) statement;
            strStatement = "result = " + strStatement;
            return interpreter.eval(strStatement);
        } catch (Error | EvalError evalError) {
            evalError.printStackTrace();
            throw new RuntimeException(Config.LOGCAT_RUNTIME_ERROR + "Could not parse Statement \"" + statement + "\"");
        }
    }

    private void loadParameterMap(ContextWrapper contextWrapper) {
        InputStream input = contextWrapper.getResources().openRawResource(R.raw.parameter_values);
        Yaml yaml = new Yaml();
        LinkedHashMap<String, Object> parameterValues = (LinkedHashMap<String, Object>) yaml.load(input);
        LinkedHashMap<String, Object> primitiveParameterMap = (LinkedHashMap<String, Object>) parameterValues.get("primitiveParameterValues");
        LinkedHashMap<String, Object> parameterStatementsMap = (LinkedHashMap<String, Object>) parameterValues.get("parameterStatements");

        predefinedParameterValues = primitiveParameterMap;

        for (Map.Entry<String, Object> parameterStatement : parameterStatementsMap.entrySet()) {
            if (parameterStatement.getValue() instanceof String) {
                Object interpreterResult = interpretStatement(parameterStatement.getValue());
                Log.v(TAG, "interpreterResult = " + interpreterResult + " for " + parameterStatement.getKey());
                Object returnValue = predefinedParameterValues.put(parameterStatement.getKey(), interpreterResult);
                if (returnValue != null) {
                    throw new RuntimeException(Config.LOGCAT_RUNTIME_ERROR + "Value exists twice: " + parameterStatement.getKey());
                }
            } else {
                LinkedHashMap<String, LinkedHashMap<String, Object>> packageAndClassMap = (LinkedHashMap<String, LinkedHashMap<String, Object>>) predefinedParameterValues.getOrDefault(parameterStatement.getKey(), new LinkedHashMap<>());
                for (Map.Entry<String, LinkedHashMap<String, Object>> methodEntry : ((LinkedHashMap<String, LinkedHashMap<String, Object>>) parameterStatement.getValue()).entrySet()) {
                    LinkedHashMap<String, Object> methodMap = packageAndClassMap.getOrDefault(methodEntry.getKey(), new LinkedHashMap<>());
                    for (Map.Entry<String, Object> parameterEntry : methodEntry.getValue().entrySet()) {
                        Object interpreterResult = interpretStatement(parameterEntry.getValue());
                        Log.v(TAG, "interpreterResult = " + interpreterResult + " for " + parameterStatement.getKey());
                        Object returnValue = methodMap.put(parameterEntry.getKey(), interpreterResult);
                        if (returnValue != null) {
                            throw new RuntimeException(Config.LOGCAT_RUNTIME_ERROR + "Value exists twice: " + parameterStatement.getKey());
                        }
                    }
                    packageAndClassMap.put(methodEntry.getKey(), methodMap);
                }
                primitiveParameterMap.put(parameterStatement.getKey(), packageAndClassMap);
            }
        }

        for (Map.Entry<String, Object> customParameterObject : loadCustomParameterObjects(contextWrapper).entrySet()) {
            Object returnValue = predefinedParameterValues.put(customParameterObject.getKey(), customParameterObject.getValue());
            if (returnValue != null) {
                throw new RuntimeException(Config.LOGCAT_RUNTIME_ERROR + "Value exists twice: " + customParameterObject.getKey());
            }
        }

        Log.v(TAG, "predefinedParameterValues = " + predefinedParameterValues);
    }

    @Nullable
    Object getPredefinedParameterValue(String packageAndClass, String methodName, String parameterName) {
        if (predefinedParameterValues.containsKey(packageAndClass)) {
            LinkedHashMap<String, Object> classInfos = (LinkedHashMap<String, Object>) predefinedParameterValues.get(packageAndClass);
            if (classInfos.containsKey(methodName)) {
                LinkedHashMap<String, Object> methodInfos = (LinkedHashMap<String, Object>) classInfos.get(methodName);
                if (methodInfos.containsKey(parameterName)) {
                    Log.v(TAG, "found parameter: " + packageAndClass + " " + methodName + " " + parameterName + " " + methodInfos.get(parameterName));
                    return methodInfos.get(parameterName);
                }
            }

            if (classInfos.containsKey("*")) {
                LinkedHashMap<String, Object> methodInfos = (LinkedHashMap<String, Object>) classInfos.get("*");
                if (methodInfos.containsKey(parameterName)) {
                    Log.v(TAG, "found parameter by wildcard: " + packageAndClass + " " + methodName + " " + parameterName + " " + methodInfos.get(parameterName));
                    return methodInfos.get(parameterName);
                }
            }
        } else if (predefinedParameterValues.containsKey(parameterName)) {
            Log.v(TAG, "found parameter without class: " + parameterName + " " + predefinedParameterValues.get(parameterName));
            return predefinedParameterValues.get(parameterName);
        }
        return null;
    }
}
