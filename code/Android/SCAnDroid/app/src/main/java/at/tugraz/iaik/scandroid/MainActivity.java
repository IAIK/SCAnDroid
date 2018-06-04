package at.tugraz.iaik.scandroid;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.usage.NetworkStatsManager;
import android.app.usage.StorageStatsManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.TrafficStats;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Process;
import android.os.storage.StorageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import at.tugraz.iaik.scandroid.services.APIHarvestingService;

public class MainActivity extends AppCompatActivity {
    // API 26+ (Android O+) only
    @Nullable
    private static List<String> getParameterNames(Method method) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            java.lang.reflect.Parameter[] parameters = method.getParameters();
            List<String> parameterNames = new ArrayList<>();

            for (java.lang.reflect.Parameter parameter : parameters) {
                if (!parameter.isNamePresent()) {
                    System.out.println("PARAMETERNAME unknown");
                    parameterNames.add("UNKNOWN");
                    continue;
                }
                parameterNames.add(parameter.getName());
            }
            return parameterNames;
        }
        return null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case 0:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    System.out.println("granted!");
                } else {
                    System.out.println("not granted!");
                }
                break;

            default:
                break;
        }
    }

    //Activity for testing various things
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActivityManager am = getSystemService(ActivityManager.class);
        for(int i = 10795; i < 10796;) {
            int[] pids = new int[1];
            pids[0] = 10795;
            Debug.MemoryInfo[] memInfo = am.getProcessMemoryInfo(pids);
            System.out.println("memInfo = " + memInfo[0].getMemoryStats());
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        /*while (true) {
            System.out.println("getMobileTxBytes " + TrafficStats.getMobileTxBytes());
            if(false)
                break;
        }*/

        NetworkStatsManager nwm = getSystemService(NetworkStatsManager.class);
        //System.out.println(((TelephonyManager)getSystemService("phone")).getSubscriberId());
        //TelephonyManager manager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //manager.getDataActivity();
        //@SuppressLint("MissingPermission") String subscriberId = manager.getSubscriberId();
        /*try {
            //NetworkStats.Bucket b = nwm.querySummaryForDevice(0, subscriberId, System.currentTimeMillis() - 100000, System.currentTimeMillis());
            //System.out.println("b.getTxBytes() = " + b.getTxBytes());

        } catch (RemoteException e) {
            e.printStackTrace();
        }*/

        //TelephonyManager telephonyManager = getSystemService(TelephonyManager.class);
        //System.out.println("telephonyManager.getDataActivity() = " + telephonyManager.getDataActivity());

        StorageManager storageManager = getSystemService(StorageManager.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                System.out.println("storageManager.getAllocatableBytes " + storageManager.getAllocatableBytes(UUID.fromString("41217664-9172-527a-b3d5-edabb50a7d69")));
                System.out.println("storageManager.getCacheQuotaBytes " + storageManager.getCacheQuotaBytes(UUID.fromString("41217664-9172-527a-b3d5-edabb50a7d69")));
                System.out.println("storageManager.getCacheSizeBytes " + storageManager.getCacheSizeBytes(UUID.fromString("41217664-9172-527a-b3d5-edabb50a7d69")));
            }
        } catch (IOException e) {
            System.out.println("e = " + e);
        }
        StorageStatsManager storageStatsManager = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            storageStatsManager = getSystemService(StorageStatsManager.class);
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                long l = storageStatsManager.getFreeBytes(UUID.fromString("41217664-9172-527a-b3d5-edabb50a7d69"));
                System.out.println("storageStatsManager.getFreeBytes = " + l);
            }

            /*StorageStats storageStats = storageStatsManager.queryStatsForUid(UUID.fromString("41217664-9172-527a-b3d5-edabb50a7d69"), 10062);
            System.out.println("storageStats.getAppBytes() " + storageStats.getAppBytes());
            System.out.println("storageStats.getCacheBytes() " + storageStats.getCacheBytes());
            System.out.println("storageStats.getDataBytes() " + storageStats.getDataBytes());*/
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Process.getElapsedCpuTime() " + Process.getElapsedCpuTime());

        StorageManager sm = getSystemService(StorageManager.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                System.out.println("sm.getAllocatableBytes " + sm.getAllocatableBytes(UUID.fromString("41217664-9172-527a-b3d5-edabb50a7d69")));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("getFilesDir().getUsableSpace() " + getFilesDir().getUsableSpace());
        System.out.println("getFilesDir().getFreeSpace() " + getFilesDir().getFreeSpace());

        long l = TrafficStats.getMobileRxBytes();
        System.out.println("TrafficStats.getMobileRxBytes = " + l);
        System.out.println("TrafficStats.getMobileRxPackets = " + TrafficStats.getMobileRxPackets());
        System.out.println("TrafficStats.getMobileTxBytes = " + TrafficStats.getMobileTxBytes());
        System.out.println("TrafficStats.getMobileTxPackets = " + TrafficStats.getMobileTxPackets());
        System.out.println("TrafficStats.getTotalRxBytes() = " + TrafficStats.getTotalRxBytes());
        System.out.println("TrafficStats.getTotalRxPackets() = " + TrafficStats.getTotalRxPackets());
        System.out.println("TrafficStats.getTotalTxBytes() = " + TrafficStats.getTotalTxBytes());
        System.out.println("TrafficStats.getTotalTxPackets() = " + TrafficStats.getTotalTxPackets());

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.BATTERY_STATS);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BATTERY_STATS}, 0);
        } else {
            System.out.println("already granted!");
        }
        //SystemHealthManager shm = getSystemService(SystemHealthManager.class);
        //System.out.println("shm.take = " + shm.takeUidSnapshot(10062));

        BatteryManager bm = getSystemService(BatteryManager.class);
        //for (; ; ) {
        // BatteryManager.BATTERY_PROPERTY_CURRENT_NOW
        //int i = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
        //System.out.println("i = " + i);
        //long l = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER);
        //System.out.println("l = " + l);
        //if (false)
        //    break;
        //}

        /*Scroller scroller = new Scroller(this);
        while(true) {
            System.out.println("getCurrVelocity = " + scroller.getCurrVelocity());
            if(false)
                break;
        }*/

        /*NetworkStatsManager nwm = (NetworkStatsManager) getSystemService(Context.NETWORK_STATS_SERVICE);
        try {
            NetworkStats ns = nwm.queryDetailsForUid(0, null, System.currentTimeMillis() - 100000, System.currentTimeMillis() + 1000 * 60 * 60, 10101);
            while (true) {
                NetworkStats.Bucket b = nwm.querySummaryForDevice(1, null, System.currentTimeMillis() - 100000, System.currentTimeMillis() + 1000 * 60 * 60);
                System.out.println("b = " + b.getTxBytes());
                if (false)
                    break;
            }
            System.out.println("ns = " + ns);
        } catch (RemoteException e) {
            e.printStackTrace();
        }*/

        /*HealthStats healthStats = new HealthStats("android.os.health.HealthStats");
        try {
            Class c = Class.forName("android.os.health.HealthStats");
            Constructor[] constructors = c.getDeclaredConstructors();
            for(Constructor co : constructors) {
                co.setAccessible(true);
                //co.newInstance()
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        //HealthStats.getStats(int);

        /*Activity newActivity = new Activity();
        Context context = newActivity.getApplicationContext();
        NetworkStatsManager nwm = (NetworkStatsManager) context.getSystemService(Context.NETWORK_STATS_SERVICE);
        try {
            NetworkStats.Bucket b = nwm.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE, null, System.currentTimeMillis() - 100000, System.currentTimeMillis());
            System.out.println("bucket = " + b);
        } catch (RemoteException e) {
            e.printStackTrace();
        }*/
        for (Constructor c : String.class.getConstructors()) {
            System.out.println("c.getName() = " + c.getName());
        }

        final Activity activity = this;

        for (Method m : String.class.getMethods()) {
            System.out.println("m.getName() = " + m.getName());
            List<String> pn = getParameterNames(m);
            if (pn != null) {
                for (String s : pn) {
                    System.out.println("strings = " + s);
                }
            }
        }

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            Intent mServiceIntent = new Intent(activity, APIHarvestingService.class);
            activity.startService(mServiceIntent);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
