package com.example.emulatordetector;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.function.Function;

public class MainActivity extends AppCompatActivity {

    TextView txtMain;

    private String[] buildModels= {
            "android sdk built for x86",
            "emulator",
            "google_sdk",
            "droid4x"
    };

    private String[] buildProducts = {
            "emulator",
            "simulator",
            "sdk_google",
            "google_sdk",
            "sdk",
            "sdk_x86",
            "vbox86p"
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtMain = findViewById(R.id.txtMain);
        txtMain.setMovementMethod(new ScrollingMovementMethod());

        Button btnMain = findViewById(R.id.button);
        btnMain.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                //txtMain.append("\nYour number: " + (int)(Math.random()*10));

                if (checkBuildModel() == true|
                    checkBuildProduct() == true){
                    appendNewLine("Emulator detected");
                }else{
                    appendNewLine("Emulator not detected");
                }
            }
        });
        //output text log to file maybe

    }

    public void appendNewLine(String txt){
        txtMain.append("\n"+txt);
    }

    interface Predicate {
        public boolean call(String param);
    }

    /**
     * Generic function that checks an array of strings using the predicate function and outputs message to the main view upon check.
     * @return True if predicate was found true at least once, else returns false.
     */
    public boolean checkArray(String[] array, Predicate checkFunction, String message){
        boolean ret = false;
        for (int i = 0; i < array.length; i++){
            if( checkFunction.call((array[i])) ){
                appendNewLine( message + array[i]);
                ret = true;
            }
        }
        return ret;
    }

    /**
     * Checks for common emulator indicators in Build.MODEL
     * @return True if emulator is detected
     */
    public boolean checkBuildModel(){
        return checkArray(buildModels, (String txt)->{return Build.MODEL.toLowerCase().contains(txt); }, "Build model contains ");
    }
    public boolean checkBuildProduct(){
        return checkArray(buildProducts, (String txt)->{return Build.PRODUCT.toLowerCase().contains(txt); }, "Build product contains ");
    }
}