package com.example.emulatordetector;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.OutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Function;

public class MainActivity extends AppCompatActivity {

    /*--------------------------------- C CODE SETUP ---------------------------------*/
    static {
        System.loadLibrary("native-lib");
    }
    private native String getNativeString();


    /*------------------------------------------------------------------*/

    private TextView txtMain;
    private static final int ALL_PERMISSIONS = 1;

    public static Map<String,Boolean> flags = new HashMap<>();
    public final int DETECTION_THRESHOLD = 3;

    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_CONTACTS
    };

    private static final String[] BUILD_MODELS = {
            "android sdk built for x86",
            "android sdk built for x86_64",
            "emulator",
            "google_sdk",
            "droid4x",
            "sdk",
            "tiantianvm",
    };

    private static final String[] BUILD_PRODUCTS = {
            "emulator",
            "simulator",
            "sdk_google",
            "google_sdk",
            "sdk",
            "sdk_x86",
            "vbox86p",
            "nox"
    };

    private static final String[] BUILD_FINGERPRINTS = {
            "vsemu",
            "generic/sdk/generic",
            "generic_x86/sdk_x86/generic_x86",
            "generic/google_sdk/generic",
            "generic/vbox86p/vbox86p",
            "generic_x86_64",
            "ttvm_hdragon",
            "vbox86p"
    };

    private static final String[] BUILD_HARDWARE = {
            "goldfish",
            "ranchu",
            "vbox86",
            "nox",
            "ttvm_x86"
    };

    private static final String[] BUILD_MANUFACTURERS = {
            "Genymotion",
            "MIT",
            "nox",
            "TiantianVM"
    };

    private static final String[] BUILD_HOSTS = {
            "apa27.mtv.corp.google.com",
            "android-test-15.mtv.corp.google.com",
            "android-test-13.mtv.corp.google.com",
            "android-test-25.mtv.corp.google.com",
            "android-test-26.mtv.corp.google.com",
            "vpbs30.mtv.corp.google.com",
            "vpak21.mtv.corp.google.com"
    };

    private static final String[] DEVICE_IDS = {
            "000000000000000",
            "e21833235b6eef10",
            "012345678912345"
    };

    private static final String[] LINE_NUMBERS = { //numbers starting with 155552155 and ending with any even number from 54-84 are emulators
            "15555215554", "15555215556", "15555215558", "15555215560", "15555215562", "15555215564",
            "15555215566", "15555215568", "15555215570", "15555215572", "15555215574", "15555215576",
            "15555215578", "15555215580", "15555215582", "15555215584"
    };

    private static final int[] SENSOR_TYPES = {
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_LIGHT,
            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_PROXIMITY
    };


    public void appendNewLine(String txt) {
        txtMain.append("\n" + txt);
    }

    public void appendNewLine() {
        txtMain.append("\n");
    }

    public void clearText() {
        txtMain.setText("");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtMain = findViewById(R.id.txtMain);
        txtMain.setMovementMethod(new ScrollingMovementMethod());

        Button btnMain = findViewById(R.id.button);

        btnMain.setOnClickListener(view -> {

            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, ALL_PERMISSIONS);

        });
        //output text log to file maybe

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case ALL_PERMISSIONS: {
                boolean allGranted = true;
                for( int res : grantResults){
                    if(res == PackageManager.PERMISSION_DENIED ) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    //all permissions granted,proceed with program
                    executeChecks();
                } else {
                    //some permission denied
                    appendNewLine("\nPlease enable all permissions to access full checks.\n");
                }
            }
        }
    }


    /**
     * Executes all the main detection checks, called once permissions are determined
     */
    public void executeChecks(){
        test();

        boolean build = checkBuild();
        boolean telephony = checkTelephony();
        boolean sensors = checkSensors();
        boolean cpu = checkCpu();
        boolean bluetooth = checkBluetooth();

        flags.put("build", build);
        flags.put("telephony", telephony);
        flags.put("sensors", sensors);
        flags.put("cpu", cpu);
        flags.put("bluetooth", bluetooth);

        int count = 0;
        for (Map.Entry<String, Boolean> entry : flags.entrySet()){
            String key = entry.getKey();
            Boolean value = entry.getValue();

            if(value){
                count++;
            }
        }

        if (count >= DETECTION_THRESHOLD){
            appendNewLine("Emulator detected: " + count + "\n");
        }else{
            appendNewLine("Emulator not detected: " + count + "\n");
        }

    }


    interface Predicate {
        boolean call(String param);
    }

    /**
     * Generic function that checks an array of strings using the predicate function and outputs message to the main view upon check.
     * @return True if predicate was found true at least once, else returns false.
     */
    public boolean checkArray(String[] array, Predicate checkFunction, String message) {
        boolean ret = false;
        for (String s : array) {
            if (checkFunction.call(s)) {
                appendNewLine(message + " '" + s + "'");
                ret = true;
            }
        }
        return ret;
    }


    private boolean checkBuildModel() {
        return checkArray(BUILD_MODELS, (String txt) -> Build.MODEL.toLowerCase().contains(txt), "Build model contains");
    }

    private boolean checkBuildProduct() {
        return checkArray(BUILD_PRODUCTS, (String txt) -> Build.PRODUCT.toLowerCase().contains(txt), "Build product contains");
    }

    private boolean checkBuildFingerprint() {
        return checkArray(BUILD_FINGERPRINTS, (String txt) -> Build.FINGERPRINT.toLowerCase().contains(txt), "Build fingerprint starts with");
    }

    private boolean checkBuildHardware() {
        return checkArray(BUILD_HARDWARE, (String txt) -> Build.HARDWARE.toLowerCase().contains(txt), "Build hardware contains");
    }

    private boolean checkBuildHosts() {
        return checkArray(BUILD_HOSTS, Build.HOST::equals, "Build host equals");
    }

    private boolean checkBuildManufacturer() {
        boolean ret = checkArray(BUILD_MANUFACTURERS, Build.MANUFACTURER::contains, "Build manufacturer contains");
        if (Build.MANUFACTURER.equalsIgnoreCase("unknown")) {
            appendNewLine("Build manufacturer equals 'unknown'");
            ret = true;
        }
        return ret;
    }

    /**
     * Checks for common emulator indicators in Build.
     * Outputs to screen what indicators it finds.
     * @return True if emulator is detected
     */
    public boolean checkBuild() {
        boolean ret = (checkBuildModel() | checkBuildProduct() | checkBuildHardware() | checkBuildFingerprint() | checkBuildHosts() | checkBuildManufacturer());

        if (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) {
            appendNewLine("Build brand and build device start with 'generic'");
            ret = true;
        }

        return ret;
    }


    /**
     * Checks IMEI numbers and device IDs using the telephony manager
     * Tells you if permissions not granted for telephony checks
     * @return True if emulator is detected
     */
    public boolean checkTelephony() {
        boolean ret = false;
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            appendNewLine("Permissions not granted to access telephony IDs.");
        }else{
            try {
                ret = checkArray(DEVICE_IDS, (String txt) -> tm.getDeviceId().equalsIgnoreCase(txt), "Device ID equals") |
                        checkArray(LINE_NUMBERS, (String txt) -> tm.getLine1Number().equals(txt), "Line1 number equals");
                if (tm.getSubscriberId().equals("310260000000000")) {
                    appendNewLine("Subscriber ID equals '310260000000000'");
                    ret = true;
                }
                if (tm.getVoiceMailNumber().equals("15552175049")) {
                    appendNewLine("Voicemail number equals '15552175049'");
                    ret = true;
                }
            } catch (Exception e) {
                appendNewLine("Exception caught: "+e);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    appendNewLine("\nSDK 29+ Cannot grant read privileged phone permission to access non-resettable ids.");
                }
            }
        }

        return ret;
    }

    /**
     * Checks for 'goldfish' keyword in sensor names
     * @return True if emulator is detected (aka virtual 'goldfish' sensor found)
     */
    private boolean checkGoldfishSensor(SensorManager sm){
        List<Sensor> sensorList = sm.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor : sensorList) {
            if (sensor.getName().toLowerCase().contains("goldfish")) {
                appendNewLine("Listed sensor names contain 'goldfish'");
                return true;
            }
        }
        return false;
    }

    /**
     * Goes through list of sensor types to check if sensor exists
     * @return True if emulator is detected (aka some sensor is not found)
     */
    private boolean checkCommonSensors(SensorManager sm , int[] sensorTypes) {
        boolean ret = false;
        for(int type : sensorTypes){
            if(sm.getDefaultSensor(type) == null){
                appendNewLine("No sensor detected for sensor type " + type);
                ret = true;
            }
        }
        return ret;
    }

    /**
     * Uses sensor manager to check for common emulator indicators in sensors
     * @return True if emulator is detected
     */
    public boolean checkSensors(){
        boolean ret = false;
        SensorManager sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        try {
            ret = checkGoldfishSensor(sm) | checkCommonSensors(sm, SENSOR_TYPES);
        }catch(Exception e){
            appendNewLine("Exception caught: "+e);
            appendNewLine("\nCannot access sensors");
        }
        return ret;
    }


    /**
     * Checks whether cpuinfo min and max freq files exist and if there are integer values in them
     * @return True if emulator is detected (files not found)
    */
    private boolean checkCpuFrequencies(){
        String minFreq = execCommand("cat /sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq");
        Scanner minScanner = new Scanner(minFreq);
        String maxFreq = execCommand("cat /sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq");
        Scanner maxScanner = new Scanner(maxFreq);
        boolean ret = false;

        if(!minScanner.hasNextInt() || !maxScanner.hasNextInt()){
            appendNewLine("CPU frequencies not found");
            ret = true;
        }
        minScanner.close();
        maxScanner.close();
        return ret;
    }

    private boolean checkCpuInfo(){
        String cpuinfoOutput = execCommand("cat /proc/cpuinfo").toLowerCase();
        if(cpuinfoOutput.contains("goldfish")){
            appendNewLine("cpuinfo contains 'goldfish'");
            return true;
        }else if(cpuinfoOutput.contains("virtual cpu")){
            appendNewLine("cpuinfo contains 'virtual cpu'");
            return true;
        }
        return false;
    }

    public boolean checkCpu(){
        return checkCpuFrequencies() | checkCpuInfo();
    }

    //this needs permissions actually
    public boolean checkDrivers(){
        String driversOutput = execCommand("cat /proc/tty/drivers").toLowerCase();
        appendNewLine(driversOutput);

        if(driversOutput.contains("goldfish")){
            appendNewLine("/proc/tty/drivers contains 'goldfish'");
            return true;
        }
        return false;
    }

    /**
     * Checks if there is bluetooth capabilities
     * @return true if no adapter found / likely an emulator
     */
    public boolean checkBluetooth(){
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null){
            appendNewLine("No bluetooth adapter found");
            return true;
        }else{
            return false;
        }
    }

    public boolean test(){
        String str = String.valueOf(Environment.getRootDirectory());
        String commandOutput = execCommand("cat /proc/cpuinfo");
        /*
        "ls", null, new File(String.valueOf(Environment.getExternalStorageDirectory())) to go to a directory to do things
        ls -1 /dev/disk/by-id/
        getprop
        "cat /proc/cpuinfo"
        cat /sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq  returns a number for real device
        appendNewLine("command output: " + commandOutput);
        */

        return false;
    }



    /**
     * Execute a linux command in specified directory
     * @return Output of the command as a string
    */
    public String execCommand(String command, String [] envp, File dir){
        String ret = null;
        try{
            Process p = Runtime.getRuntime().exec(command, envp, dir);
            InputStream inputStream = p.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line + "\n");
            }

            p.waitFor();
            reader.close();
            inputStream.close();

            ret = stringBuilder.toString();

        }catch(Exception e){
            appendNewLine("Exception caught: "+e);
            appendNewLine("\nCannot run command");
        }
        return ret;
    }

    public String execCommand(String command) {
        return execCommand(command, null, null);
    }


}