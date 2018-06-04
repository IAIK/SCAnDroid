package at.tugraz.iaik.scandroid.parsedTypes;

import java.util.ArrayList;

/**
 * Created by Gerald Palfinger on 27.10.17.
 */

public class APIMethodInfo {
    private final String name;
    private final ArrayList<String[]> parameterNames = new ArrayList<>(); // method with same name and different params can exist

    APIMethodInfo(String methodName) {
        this.name = methodName;
    }

    void addNewSignature(String[] parameterName) {
        parameterNames.add(parameterName);
    }

    public String getName() {
        return name;
    }

    public ArrayList<String[]> getParameterNames() {
        return parameterNames;
    }

    public ArrayList<String[]> getParameterNamesWithLength(int length) {
        ArrayList<String[]> parameterNamesWithLength = new ArrayList<>();
        for (String[] parameterName : parameterNames) {
            if (parameterName.length == length) {
                parameterNamesWithLength.add(parameterName);
            }
        }
        return parameterNamesWithLength;
    }
}
