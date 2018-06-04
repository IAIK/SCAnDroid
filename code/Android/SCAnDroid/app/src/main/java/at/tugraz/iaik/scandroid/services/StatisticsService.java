package at.tugraz.iaik.scandroid.services;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import at.tugraz.iaik.scandroid.Config;
import at.tugraz.iaik.scandroid.Parser;

/**
 * Created by Gerald Palfinger on 07.09.17.
 */

public class StatisticsService extends IntentService {
    private static final String TAG = StatisticsService.class.getName();

    public StatisticsService() {
        super("APIHarvestingService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        int relevantMethodsInFile = 0, allMethodsInFile = 0;
        int publicRelevantMethodsFound = 0, nonPublicRelevantMethodsFound = 0, allMethodsFound = 0;
        int publicMethodsFound = 0, privateMethodsFound = 0, protectedMethodsFound = 0, somethingElse = 0;
        int foundRelevantMethodFromFile = 0;

        Config.loadConfig(this);
        BufferedReader reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(Config.methods_for_methods_dumper)));

        do {
            try {
                String currentClass = reader.readLine();

                Log.v(TAG, "currentClass = " + currentClass);

                if (currentClass == null) {
                    break;
                }

                int nrOfMethods = Parser.getNrOfMethods(reader, "Methods");
                ArrayList<String> relevantMethods = new ArrayList<>();
                for (int methodCounter = 0; methodCounter < nrOfMethods; ++methodCounter) {
                    String methodName = reader.readLine().split("\\|")[0].trim();
                    if (Config.isRelevantMethod(methodName)) {
                        ++relevantMethodsInFile;
                        relevantMethods.add(methodName);
                    }
                    ++allMethodsInFile;
                }

                Class clazz = Class.forName(currentClass);

                Method[] methodsInClass = clazz.getMethods();

                for (String relevantMethodName : relevantMethods) {
                    for (Method methodInClass : methodsInClass) {
                        if (methodInClass.getName().equals(relevantMethodName)) {
                            ++foundRelevantMethodFromFile;
                            break;
                        }
                    }
                }

                Method[] methods = clazz.getDeclaredMethods();
                for (Method method : methods) {
                    String methodName = method.getName();
                    if (Modifier.isPublic(method.getModifiers())) {
                        ++publicMethodsFound;
                    }
                    if (Modifier.isPrivate(method.getModifiers())) {
                        ++privateMethodsFound;
                    }
                    if (Modifier.isProtected(method.getModifiers())) {
                        ++protectedMethodsFound;
                    }
                    if (!Modifier.isPublic(method.getModifiers()) && !Modifier.isPrivate(method.getModifiers()) && !Modifier.isProtected(method.getModifiers())) {
                        ++somethingElse;
                    }
                    if (Modifier.isPublic(method.getModifiers()) && Config.isRelevantMethod(methodName)) {
                        ++publicRelevantMethodsFound;
                    } else if (Config.isRelevantMethod(methodName.replace("-", ""))) {
                        ++nonPublicRelevantMethodsFound;
                    }
                    ++allMethodsFound;
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        } while (true);

        Log.i(TAG, "relevantMethodsInFile = " + relevantMethodsInFile + " of allMethodsInFile = " + allMethodsInFile);
        Log.i(TAG, "publicRelevantMethodsFound = " + publicRelevantMethodsFound +
                " nonPublicRelevantMethodsFound = " + nonPublicRelevantMethodsFound + " of allMethodsFound = " + allMethodsFound);
        Log.i(TAG, "publicMethodsFound = " + publicMethodsFound + " protectedMethodsFound = " + protectedMethodsFound + " privateMethodsFound = " + privateMethodsFound + " somethingElse = " + somethingElse);
        Log.i(TAG, "foundRelevantMethodFromFile = " + foundRelevantMethodFromFile);
        Log.i(TAG, "Finished reading statistics");
    }
}
