package com.example.emulatordetector;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
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
import java.util.List;

public class MainActivity extends AppCompatActivity {


    private TextView txtMain;
    private static final int REQUEST_PHONE_STATE = 1;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtMain = findViewById(R.id.txtMain);
        txtMain.setMovementMethod(new ScrollingMovementMethod());

        //request phone permissions if not given
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            appendNewLine("Please grant phone permissions to enable full checks.");
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.READ_PHONE_STATE},
                    REQUEST_PHONE_STATE);
        }

        Button btnMain = findViewById(R.id.button);
        btnMain.setOnClickListener(view -> {
            checkSystemProduct();
            if (checkBuild() | checkTelephony() | checkSensor() ) {
                appendNewLine("Emulator detected");
            } else {
                appendNewLine("Emulator not detected");
            }
            appendNewLine();
        });
        //output text log to file maybe

    }

    public void appendNewLine(String txt) {
        txtMain.append("\n" + txt);
    }

    public void appendNewLine() {
        txtMain.append("\n");
    }

    public void clearText() {
        txtMain.setText("");
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
                    appendNewLine("SDK 29+ Cannot grant read privileged phone state permission to access non-resettable ids.");
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
                appendNewLine("Listed sensor name contains 'goldfish'");
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
    public boolean checkSensor(){
        boolean ret = false;
        SensorManager sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        try {
            ret = checkGoldfishSensor(sm) | checkCommonSensors(sm, SENSOR_TYPES);
        }catch(Exception e){
            appendNewLine("Exception caught: "+e);
            appendNewLine("Cannot access sensors");
        }
        return ret;
    }

    public boolean checkSystemProduct(){
        String commandOutput = execCommand("ls", null, new File( String.valueOf(Environment.getRootDirectory())) );
        //"ls", null, new File(String.valueOf(Environment.getExternalStorageDirectory())) to go to a directory to do things
        //ls -1 /dev/disk/by-id/
        //getprop
        appendNewLine(commandOutput);

        return true;
    }

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
            appendNewLine("Can't run command: " + e);
        }
        return ret;
    }

    public String execCommand(String command) {
        return execCommand(command, null, null);
    }


}