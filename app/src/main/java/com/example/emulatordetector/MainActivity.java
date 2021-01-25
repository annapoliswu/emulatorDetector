package com.example.emulatordetector;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.function.Function;

public class MainActivity extends AppCompatActivity {

    TextView txtMain;

    private String[] buildModels= {
            "android sdk built for x86",
            "emulator",
            "google_sdk",
            "droid4x",
            "sdk"
    };

    private String[] buildProducts = {
            "emulator",
            "simulator",
            "sdk_google",
            "google_sdk",
            "sdk",
            "sdk_x86",
            "vbox86p",
            "nox"
    };

    private String[] buildFingerprints = {
            "generic",
            "unknown"
    };

    private String[] buildHardware = {
            "goldfish",
            "ranchu",
            "vbox86",
            "nox"
    };

    private String[] deviceIDs = {
            "000000000000000",
            "e21833235b6eef10",
            "012345678912345"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtMain = findViewById(R.id.txtMain);
        txtMain.setMovementMethod(new ScrollingMovementMethod());

        Button btnMain = findViewById(R.id.button);
        btnMain.setOnClickListener(view -> {
            //txtMain.append("\nYour number: " + (int)(Math.random()*10));

            if (checkBuild()){
                appendNewLine("Emulator detected");
            }else{
                appendNewLine("Emulator not detected");
            }
        });
        //output text log to file maybe

    }

    public void appendNewLine(String txt){
        txtMain.append("\n"+txt);
    }

    interface Predicate {
        boolean call(String param);
    }

    /**
     * Generic function that checks an array of strings using the predicate function and outputs message to the main view upon check.
     * @return True if predicate was found true at least once, else returns false.
     */
    public boolean checkArray(String[] array, Predicate checkFunction, String message){
        boolean ret = false;
        for (String s : array) {
            if (checkFunction.call(s)) {
                appendNewLine(message + " '" + s + "'");
                ret = true;
            }
        }
        return ret;
    }


    private boolean checkBuildModel(){
        return checkArray(buildModels, (String txt)-> Build.MODEL.toLowerCase().contains(txt), "Build model contains");
    }

    private boolean checkBuildProduct(){
        return checkArray(buildProducts, (String txt)-> Build.PRODUCT.toLowerCase().contains(txt), "Build product contains");
    }

    private boolean checkBuildFingerprint(){
        return checkArray(buildFingerprints, (String txt)-> Build.FINGERPRINT.toLowerCase().startsWith(txt), "Build fingerprint starts with");
    }

    private boolean checkBuildHardware(){
        return checkArray(buildHardware, (String txt)-> Build.HARDWARE.toLowerCase().contains(txt), "Build hardware contains");
    }

    /**
     * Checks for common emulator indicators in Build.
     * Outputs to screen what indicators it finds.
     * @return True if emulator is detected
     */
    public boolean checkBuild() {
        boolean ret = (checkBuildModel() | checkBuildProduct() | checkBuildHardware() | checkBuildFingerprint());

        if (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) {
            appendNewLine("Build brand and build device start with 'generic'");
            ret = true;
        }
        if (Build.MANUFACTURER.contains("Genymotion")){
            appendNewLine("Build manufacturer contains 'Genymotion'");
            ret = true;
        }

        return ret;
    }

    /**
     * Checks IMEI numbers and device IDs using the telephony manager
     * currently crashing, working on this
     */
    public boolean checkTelephone() {
        boolean ret = false;
        TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        appendNewLine("Device id: " + tm.getDeviceId());

        /*ret = checkArray(deviceIDs, (String txt)->{return tm.getDeviceId().equalsIgnoreCase(txt); }, "Device ID equals");

        if(tm.getSubscriberId().equals("310260000000000")){
            appendNewLine("Device IMSI ID equals '310260000000000'");
            ret = true;
        }
         */
        return ret;
    }


}