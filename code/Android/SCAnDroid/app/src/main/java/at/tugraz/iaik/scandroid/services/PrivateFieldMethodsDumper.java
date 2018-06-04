package at.tugraz.iaik.scandroid.services;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import at.tugraz.iaik.scandroid.Config;
import at.tugraz.iaik.scandroid.Parser;

/**
 * Created by Gerald Palfinger on 07.09.17.
 */

public class PrivateFieldMethodsDumper extends IntentService {
    private static final String TAG = PrivateFieldMethodsDumper.class.getName();
    public PrivateFieldMethodsDumper() {
        super("APIHarvestingService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(Config.methods_for_methods_dumper)));
        File privateMethodsFile = new File(getExternalFilesDir(null), "private_methods.txt");
        File privateFieldsFile = new File(getExternalFilesDir(null), "private_fields.txt");

        FileWriter privateMethodsWriter;
        FileWriter privateFieldsWriter;
        try {
            privateMethodsWriter = new FileWriter(privateMethodsFile);
            privateFieldsWriter = new FileWriter(privateFieldsFile);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        do {
            try {
                String currentClass = reader.readLine();

                Log.v(TAG, "currentClass = " + currentClass);

                if (currentClass == null) {
                    break;
                }

                int nrOfMethods = Parser.getNrOfMethods(reader, "Methods");
                for (int methodCounter = 0; methodCounter < nrOfMethods; ++methodCounter) {
                    reader.readLine(); // ignore methods
                }

                Class clazz = Class.forName(currentClass);

                privateFieldsWriter.append(currentClass).append("\n").append(" Fields: ");
                privateMethodsWriter.append(currentClass).append("\n").append(" Methods: ");

                Field[] fields = clazz.getDeclaredFields();
                ArrayList<Field> privateFields = new ArrayList<>();
                for (Field field : fields) {
                    if (!Modifier.isPublic(field.getModifiers())) {
                        privateFields.add(field);
                    }
                }
                privateFieldsWriter.append(Integer.toString(privateFields.size())).append("\n");
                for (Field field : privateFields) {
                    privateFieldsWriter.append("  ").append(field.getName()).append("\n");
                }


                Method[] methods = clazz.getDeclaredMethods();
                ArrayList<Method> privateMethods = new ArrayList<>();
                for (Method method : methods) {
                    if (!Modifier.isPublic(method.getModifiers())) {
                        privateMethods.add(method);
                    }
                }
                privateMethodsWriter.append(Integer.toString(privateMethods.size())).append("\n");
                for (Method method : privateMethods) {
                    privateMethodsWriter.append("  ").append(method.getName()).append("\n");
                }

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        } while (true);

        Log.i(TAG, "Finished reading private fields and methods");

        try {
            privateMethodsWriter.flush();
            privateFieldsWriter.flush();
            privateMethodsWriter.close();
            privateFieldsWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
