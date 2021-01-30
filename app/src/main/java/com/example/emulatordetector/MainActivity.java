package com.example.emulatordetector;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
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
    private static final int REQUEST_PHONE_STATE = 1;

    private String[] buildModels = {
            "android sdk built for x86",
            "android sdk built for x86_64",
            "emulator",
            "google_sdk",
            "droid4x",
            "sdk",
            "tiantianvm",
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
            "vsemu",
            "generic/sdk/generic",
            "generic_x86/sdk_x86/generic_x86",
            "generic/google_sdk/generic",
            "generic/vbox86p/vbox86p",
            "generic_x86_64",
            "ttvm_hdragon",
            "vbox86p"
    };

    private String[] buildHardware = {
            "goldfish",
            "ranchu",
            "vbox86",
            "nox",
            "ttvm_x86"
    };

    private String[] buildManufacturers = {
            "Genymotion",
            "MIT",
            "nox",
            "TiantianVM"
    };

    private String[] buildHosts = {
            "apa27.mtv.corp.google.com",
            "android-test-15.mtv.corp.google.com",
            "android-test-13.mtv.corp.google.com",
            "android-test-25.mtv.corp.google.com",
            "android-test-26.mtv.corp.google.com",
            "vpbs30.mtv.corp.google.com",
            "vpak21.mtv.corp.google.com"
    };

    private String[] deviceIDs = {
            "000000000000000",
            "e21833235b6eef10",
            "012345678912345"
    };

    private String[] lineNumbers = { //numbers starting with 155552155 and ending with any even number from 54-84 are emulators
            "15555215554", "15555215556", "15555215558", "15555215560", "15555215562", "15555215564",
            "15555215566", "15555215568", "15555215570", "15555215572", "15555215574", "15555215576",
            "15555215578", "15555215580", "15555215582", "15555215584"
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
            appendNewLine();
            if (checkBuild() | checkTelephony()) {
                appendNewLine("Emulator detected");
            } else {
                appendNewLine("Emulator not detected");
            }
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
        return checkArray(buildModels, (String txt) -> Build.MODEL.toLowerCase().contains(txt), "Build model contains");
    }

    private boolean checkBuildProduct() {
        return checkArray(buildProducts, (String txt) -> Build.PRODUCT.toLowerCase().contains(txt), "Build product contains");
    }

    private boolean checkBuildFingerprint() {
        return checkArray(buildFingerprints, (String txt) -> Build.FINGERPRINT.toLowerCase().contains(txt), "Build fingerprint starts with");
    }

    private boolean checkBuildHardware() {
        return checkArray(buildHardware, (String txt) -> Build.HARDWARE.toLowerCase().contains(txt), "Build hardware contains");
    }

    private boolean checkBuildHosts() {
        return checkArray(buildHosts, Build.HOST::equals, "Build host equals");
    }

    private boolean checkBuildManufacturer() {
        boolean ret = checkArray(buildManufacturers, Build.MANUFACTURER::contains, "Build manufacturer equals");
        if (Build.MANUFACTURER.contains("unknown")) {
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
     */
    public boolean checkTelephony() {
        boolean ret = false;
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            appendNewLine("Permissions not granted to access telephony IDs.");
        }else{
            try {
                ret = checkArray(deviceIDs, (String txt) -> tm.getDeviceId().equalsIgnoreCase(txt), "Device ID equals") |
                        checkArray(lineNumbers, (String txt) -> tm.getLine1Number().equals(txt), "Line1 number equals");
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


}