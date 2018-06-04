package at.tugraz.iaik.scandroid.types;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import at.tugraz.iaik.scandroid.Config;

/**
 * Created by Gerald Palfinger on 14.09.17.
 */

public class APIMethod {
    private static final String TAG = APIMethod.class.getName();
    ;

    private Object classObject;
    private final Method method;
    private final Object[] parameterObjects;
    private final String className;
    private final File path;
    private final String hierarchy;

    private Object parentClassObject;
    private Method objectCreatorMethod;
    private Object[] objectCreatorMethodParameters;

    private StringBuilder measurementsStringStream = new StringBuilder();
    private int nrOfAppendsToStringStream = 0;
    private boolean addedMethodHierarchy = false;

    private static int FILES_CREATED = 0;
    public static boolean KEEP_MEASUREMENTS = false;

    private File measurementsFile;
    private FileOutputStream measurementsFileStream;

    public long firstTimestamp;
    public String previouslyWrittenToFile = "";
    private boolean recreateObject;

    APIMethod(Context context, Object classObject, Method method, Object[] parameterObjects, String className, String hierarchy) {
        path = context.getExternalFilesDir(null);
        this.method = method;
        this.parameterObjects = parameterObjects;
        this.className = className;
        this.hierarchy = hierarchy;
        this.classObject = classObject;
    }

    public boolean hasParentCreators() {
        return parentClassObject != null || objectCreatorMethod != null || objectCreatorMethodParameters != null;
    }

    public void setParentCreators(Object parentClassObject, Method objectCreatorMethod, Object[] objectCreatorMethodParameters) {
        this.parentClassObject = parentClassObject;
        this.objectCreatorMethod = objectCreatorMethod;
        this.objectCreatorMethodParameters = objectCreatorMethodParameters;
    }

    public Object getClassObject() {
        if (objectCreatorMethod != null) {
            try {
                Log.v(TAG, "Recreating class object for " + className);
                classObject = objectCreatorMethod.invoke(parentClassObject, objectCreatorMethodParameters);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
                throw new RuntimeException(Config.LOGCAT_RUNTIME_ERROR + "Could not recreate class object");
            }
        }
        return classObject;
    }

    public void addTargetToStringStream(String target) {
        if (!Config.HAR_STYLE_LOG_FILES) {
            writeMeasurement(target + ", \n");
        } else {
            writeMeasurement("\n");
        }
    }

    public void addMeasurement(String measurement, String time) {
        if (!Config.HAR_STYLE_LOG_FILES) {
            writeMeasurement(measurement + "|" + time + ", ");
        } else {
            writeMeasurement("  " + measurement);
        }
    }

    private void writeMeasurement(String data) {
        ++nrOfAppendsToStringStream;
        if (!KEEP_MEASUREMENTS) {
            return;
        }
        try {
            if (!Config.USE_STRING_STREAM && measurementsFile == null) {
                createFiles();
            }

            if (!addedMethodHierarchy) {
                if (Config.USE_STRING_STREAM) {
                    measurementsStringStream.append(hierarchy);
                    measurementsStringStream.append("\n");
                } else {
                    measurementsFileStream.write(hierarchy.getBytes());
                    measurementsFileStream.write("\n".getBytes());
                }
                addedMethodHierarchy = true;
            }

            if (Config.USE_STRING_STREAM) {
                measurementsStringStream.append(data);
            } else {
                measurementsFileStream.write(data.getBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @NonNull
    private String getSignature() {
        StringBuilder signature = new StringBuilder();
        signature.append(className);
        signature.append("_");
        signature.append(method.getName());
        signature.append("(");
        for (int counter = 0; counter < parameterObjects.length; ++counter) {
            Object parameterObject = parameterObjects[counter];
            if (counter > 0) {
                signature.append(",");
            }
            signature.append(parameterObject.getClass().getName());
        }
        signature.append(")");
        return signature.toString();
    }

    public void writeToFile() {
        try {
            if (measurementsFile == null) {
                createFiles();
            }
            measurementsFileStream.write(measurementsStringStream.toString().getBytes());
            clearStringStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createFiles() throws FileNotFoundException {
        Random random = new Random();
        int randomInt = random.nextInt();
        measurementsFile = new File(path, getSignature() + "_" + randomInt + ".txt");
        measurementsFileStream = new FileOutputStream(measurementsFile);
        ++FILES_CREATED;
    }

    public Method getMethod() {
        return method;
    }

    private Object[] recreateParameterObjects() {
        return null;
    }

    public Object[] getParameterObjects() {
        return recreateObject ? recreateParameterObjects() : parameterObjects;
    }

    private void clearStringStream() {
        measurementsStringStream = new StringBuilder();
        nrOfAppendsToStringStream = 0;
    }

    public boolean stringStreamChangedMoreThanOnce() {
        return nrOfAppendsToStringStream > 1;
    }

    @Override
    public String toString() {
        return "method: " + method.getName() + " of class " + className;
    }
}
