/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.blockly.android.demo;

import android.Manifest;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.net.DhcpInfo;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.blockly.android.AbstractBlocklyActivity;
import com.google.blockly.android.BlocklySectionsActivity;
import com.google.blockly.android.codegen.CodeGenerationRequest;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.ui.BlockViewFactory;
import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.android.ui.vertical.VerticalBlockViewFactory;
import com.google.blockly.model.BlocklySerializerException;
import com.google.blockly.model.Workspace;
import com.google.blockly.util.JavascriptUtil;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;


/**
 * Demo app with the Blockly Games turtle game in a webview.
 */



public class TurtleActivity extends AbstractBlocklyActivity {




    final String PREFS_NAME = "MyPrefsFile";

    WiFiConnectionReciever rcv;
    int UDP_SERVER_PORT = 8266;
    int MAX_UDP_DATAGRAM_LEN = 4096;
    String BROADCAST_ADDRESS = "192.168.43.255";
    private RunServerInThread runServer = null;
    private int OTAstate = 0;

    Handler timeoutOTA = new Handler();
    int OTAtimeOutRetryCounter = 0;
    Handler timeoutUDP = new Handler(Looper.getMainLooper());
    int UDPtimeOutRetryCounter = 0;

    Handler UDPDelayTimer = new Handler(Looper.getMainLooper());




    private int ttout = 10000;

    String assetPath;

    InetAddress boardIP = null;



    CmdExecResult makeCompleteCallback = new makeCompleteCallbackClass();

    AlertDialog complettionDialog;

    private static final String TAG = "TurtleActivity";

    public BlocklyController mController;

    public static final String SAVED_WORKSPACE_FILENAME = "turtle_workspace.xml";
    static final List<String> TURTLE_BLOCK_DEFINITIONS = Arrays.asList(new String[]{
            "blocks.json"
    });
    static final List<String> TURTLE_BLOCK_GENERATORS = Arrays.asList(new String[]{
            "kid-bright.js",
            "generators.js"
    });


    private final Handler mHandler = new Handler();

    private final CodeGenerationRequest.CodeGeneratorCallback mCodeGeneratorCallback =
            new CodeGenerationRequest.CodeGeneratorCallback() {
                @Override
                public void onFinishCodeGeneration(final String generatedCode) {
                    // Sample callback.
                    Log.d("HTTP", "generatedCode:\n" + generatedCode);


                    runOnUiThread(new Runnable() {
                        public void run() {


                            LayoutInflater inflater = getLayoutInflater();
                            View progressLayout = inflater.inflate(R.layout.pregress, null);

                            AlertDialog.Builder alert = new AlertDialog.Builder(TurtleActivity.this, R.style.ThemeDialogCustom);
                            alert.setTitle("Progress indicator");
                            // this is set the view from XML inside AlertDialog
                            alert.setView(progressLayout);
                            // disallow cancel of AlertDialog on click of back button and outside touch
                            alert.setCancelable(false);

                            alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    complettionDialog.dismiss();

                                }
                            });



                            complettionDialog = alert.create();
                            complettionDialog.show();
                            complettionDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);



                            CheckBox codeGenComplete = (CheckBox)  progressLayout.findViewById(R.id.generateCodeCheckBox);
                            codeGenComplete.setChecked(true);


                        }
                    });







                 /*  TurtleActivity.runOnUiThread(new Runnable() {
                        public void run() {
                            //something here



                            CheckBox codeGenComplete = (CheckBox)  progressLayout.findViewById(R.id.compilingCheckBox);
                            codeGenComplete.setChecked(true);
                        }
                    });*/




                    //Toast.makeText(getApplicationContext(), generatedCode,
                      //      Toast.LENGTH_LONG).show();


                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            File file = new File(assetPath + "template/user/user_app.c");
                            if (!file.exists()) {
                                //file.createNewFile();
                            } else {
                                Log.d("HTTP", "File exist!!!!!!!");
                            }

                            try {
                                //FileOutputStream writer = openFileOutput(file, Context.MODE_PRIVATE);
                                FileOutputStream writer = new FileOutputStream(file);


                                writer.write(generatedCode.getBytes());
                                writer.flush();
                                writer.close();




                                try {
                                    execCmdSync(assetPath + "xtensa-lx106-elf_arm/bin/make -C " + assetPath+"template", makeCompleteCallback);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }


                                String[] command = new String[]{"/system/bin/ls", "-l",
                                        assetPath + "template/out"};
                                Process process = Runtime.getRuntime().exec(command);
                                BufferedReader reader = new BufferedReader(
                                        new InputStreamReader(process.getInputStream()));
                                int read;

                                String output = "";

                                String line;
                                while ((line = reader.readLine()) != null) {
                                    output.concat(line + "\n");
                                    Log.w("HTTP", "[[output]]:" + line);
                                }
                                reader.close();
                                process.waitFor();


                                //Start OTA Here

                                initOTA();





                            } catch (IOException e) {
                                    e.printStackTrace();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }


                        }
                    });
                }




            };

    @Override
    public void onLoadWorkspace() {

        Log.d("HTTP", "on load ");





        runOnUiThread(new Runnable() {
                          public void run() {

                              AlertDialog.Builder builderSingle = new AlertDialog.Builder(TurtleActivity.this, R.style.ThemeDialogCustom);
                              //builderSingle.setIcon(R.drawable.ic_launcher);
                              builderSingle.setTitle("Select a workspace file");

                              final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                                      TurtleActivity.this,
                                      android.R.layout.select_dialog_singlechoice);


                              File f = new File(assetPath + "user_workspace");
                              File file[] = f.listFiles();

                              for (File f1 : file) {
                                  //if (f1.isDirectory()) {
                                  Log.d("HTTP", f1.getAbsolutePath());

                                  String ppstring = f1.getAbsolutePath();


                                  arrayAdapter.add(ppstring.substring(ppstring.lastIndexOf("/") + 1));



                                  //Log.d("HTTP", f1.getAbsolutePath());
                                  //}
                                  // Do your stuff
                              }



                              builderSingle.setNegativeButton(
                                      "cancel",
                                      new DialogInterface.OnClickListener() {
                                          @Override
                                          public void onClick(DialogInterface dialog, int which) {
                                              dialog.dismiss();
                                          }
                                      });

                              builderSingle.setAdapter(
                                      arrayAdapter,
                                      new DialogInterface.OnClickListener() {
                                          @Override
                                          public void onClick(DialogInterface dialog, int which) {
                                              String strName = arrayAdapter.getItem(which);

                                              File file = new File(assetPath + "user_workspace/" + strName);

                                              FileInputStream fos = null;
                                              try {
                                                  fos = new FileInputStream(file);
                                                  mController.loadWorkspaceContents(fos);


                                              } catch (FileNotFoundException e) {
                                                  e.printStackTrace();
                                              }





                                          }
                                      });
                              builderSingle.show();
                          }
                      });




        //loadWorkspaceFromAppDir(SAVED_WORKSPACE_FILENAME);




        //TO DO add choose file dialog
    }

    @Override
    public void onSaveWorkspace() {

        //super.onSaveWorkspace();
        Log.d("HTTP", "on save ");

        //saveWorkspaceToAppDir(SAVED_WORKSPACE_FILENAME);


        final Workspace workspace = mWorkspaceFragment.getWorkspace();


        runOnUiThread(new Runnable() {
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(TurtleActivity.this, R.style.ThemeDialogCustom);

                builder.setTitle("Workspace filename");

                // Set up the input
                final EditText input = new EditText(TurtleActivity.this);
                // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                input.setInputType(InputType.TYPE_CLASS_TEXT );
                input.setHint("Filename");
                builder.setView(input);

                // Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String m_Text = input.getText().toString();


                        try {

                            File file = new File(assetPath + "user_workspace/", m_Text + ".xml");
                            FileOutputStream fos = new FileOutputStream(file);

                            workspace.serializeToXml(fos); //openFileOutput(fos, Context.MODE_PRIVATE));
                            // workspace.serializeToXml();
                            Toast.makeText(getApplicationContext(), "Done save workspace!!!!",
                                    Toast.LENGTH_LONG).show();


                        } catch (FileNotFoundException | BlocklySerializerException e) {
                            Toast.makeText(getApplicationContext(), R.string.toast_workspace_not_saved,
                                    Toast.LENGTH_LONG).show();
                        }

                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();


            }
        });






        //TO DO add choose file dialog

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return onDemoItemSelected(item, this) || super.onOptionsItemSelected(item);
    }

    boolean onDemoItemSelected(MenuItem item, AbstractBlocklyActivity activity) {
        BlocklyController controller = activity.getController();
        int id = item.getItemId();
        boolean loadWorkspace = false;
        String filename = "";
        if (id == R.id.action_demo_android) {


            runOnUiThread(new Runnable() {
                public void run() {



                    AlertDialog.Builder builder = new AlertDialog.Builder(TurtleActivity.this, R.style.ThemeDialogCustom);

                    builder.setTitle("Enter a board ID");

                    // Set up the input
                    final EditText input = new EditText(TurtleActivity.this);

                    InputFilter inputFilter_moodMsg = new HexadecimalInputFilter(false);
                    input.setFilters(new InputFilter[] { inputFilter_moodMsg });
                    input.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);


                    // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                    input.setInputType(InputType.TYPE_CLASS_TEXT );
                    input.setHint("Board ID");
                    builder.setView(input);

                    // Set up the buttons
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String data = input.getText().toString();

                           // String data = "1ba5f6";
                            data = data + "66";

                            long x = Long.parseLong(data, 16);
                            x = x*2;

                            String hex = Long.toHexString(x);

                            Log.d("HTTP", "SSID = " + data + " , Password = " + hex);



                            if(WifiAccessManager.setWifiApState(TurtleActivity.this, input.getText().toString(), hex,  false) == true) {

                                WifiAccessManager.setWifiApState(TurtleActivity.this, input.getText().toString(), hex, true);

                                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

                                settings.edit().putString("board_id", input.getText().toString()).commit();




                            }



                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    builder.show();


                }
            });




        } else if (id == R.id.action_demo_lacey_curves) {

            Log.d("HTTP","Menu selected");

        } else if (id == R.id.action_demo_paint_strokes) {
            loadWorkspace = true;
            filename = "paint_strokes.xml";
        }

        if (loadWorkspace) {
            try {
                controller.loadWorkspaceContents(activity.getAssets().open(
                        "turtle/demo_workspaces/" + filename));
            } catch (IOException e) {
                Log.d(TAG, "Couldn't load workspace from assets");
            }
            addDefaultVariables(controller);
            return true;
        }

        return false;
    }

    @NonNull
    @Override
    protected List<String> getBlockDefinitionsJsonPaths() {
        // Use the same blocks for all the levels. This lets the user's block code carry over from
        // level to level. The set of blocks shown in the toolbox for each level is defined by the
        // toolbox path below.
        return TURTLE_BLOCK_DEFINITIONS;
    }

    @Override
    protected int getActionBarMenuResId() {
        return R.menu.turtle_actionbar;
    }

    @NonNull
    @Override
    protected List<String> getGeneratorsJsPaths() {
        return TURTLE_BLOCK_GENERATORS;
    }

    @NonNull
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);




        mController = TurtleActivity.this.getController();


     /*   File folder = new File(assetPath+ "user_workspace");
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdir();
        }
        if (success) {
            // Do something on success

            Log.d("HTTP", "Successful to create a directory");

        } else {
            // Do something else on failure
            Log.d("HTTP", "Fail to create a directory");
        }*/



        ApManager.isApOn(TurtleActivity.this); // check Ap state :boolean
        ApManager.configApState(TurtleActivity.this); // change Ap state :boolean


        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction("android.net.wifi.WIFI_HOTSPOT_CLIENTS_CHANGED");
        rcv = new WiFiConnectionReciever();
        registerReceiver(rcv, mIntentFilter);



        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

       /* if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_SETTINGS)){

        }else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_SETTINGS},
                    121);
        }*/

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        if (settings.getString("board_id", "").length() > 0) {
            // String data = "1ba5f6";
            String data = settings.getString("board_id", "") + "66";

            long x = Long.parseLong(data, 16);
            x = x*2;

            String hex = Long.toHexString(x);

            Log.d("HTTP", "SSID = " + data + " , Password = " + hex);



            if(WifiAccessManager.setWifiApState(TurtleActivity.this,settings.getString("board_id", "") , hex,  false) == true) {

                WifiAccessManager.setWifiApState(TurtleActivity.this, settings.getString("board_id", ""), hex, true);
            }


        }



       // Log.d("HTTP", "IP Address = " + getWifiApIpAddress().getHostAddress());



        assetPath = getFilesDir().toString() + "/";


        Log.d("HTTP", assetPath);



        if (settings.getBoolean("my_first_time", true)) {
            //the app is being launched for first time, do something
            Log.d("HTTP", "First time");

            checkSystemWritePermission();

            //copyFilesToSdCard();

            copyAssetTask copyFile = new copyAssetTask();

            copyFile.execute();

            // first time task

            // record the fact that the app has been started at least once
            settings.edit().putBoolean("my_first_time", false).commit();



        }


        AsyncHttpServer server = new AsyncHttpServer();

        List<WebSocket> _sockets = new ArrayList<WebSocket>();

        server.get("/rom0.bin", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                //response.send("Hello!!!");

                Log.d("HTTP", request.toString());
                String path = assetPath + "template/out/" + "rom0.bin";



                File file = new File(path);
                response.sendFile(file);



            }
        });

        // listen on port 8000
        server.listen(8000);


        runServer = new RunServerInThread();
        runServer.start();

    }



    @NonNull
    @Override
    protected String getToolboxContentsXmlPath() {
        // Expose a different set of blocks to the user at each level.
        return "toolbox.xml";
    }

    @Override
    public BlockViewFactory onCreateBlockViewFactory(WorkspaceHelper helper) {
        return new VerticalBlockViewFactory(this, helper);
    }

    @Override
    protected void onInitBlankWorkspace() {
        addDefaultVariables(getController());
    }



   /* @Override
    protected View onCreateContentView(int parentId) {
        View root = getLayoutInflater().inflate(R.layout.turtle_content, null);

        mTurtleWebview = (WebView) root.findViewById(R.id.turtle_runtime);
        mTurtleWebview.getSettings().setJavaScriptEnabled(true);
        mTurtleWebview.setWebChromeClient(new WebChromeClient());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        mTurtleWebview.loadUrl("file:///android_asset/turtle/turtle.html");

        return root;
    }*/

    @NonNull
    @Override
    protected CodeGenerationRequest.CodeGeneratorCallback getCodeGenerationCallback() {
        return mCodeGeneratorCallback;
    }

    static void addDefaultVariables(BlocklyController controller) {
        // TODO: (#22) Remove this override when variables are supported properly
        controller.addVariable("item");
        controller.addVariable("count");
        controller.addVariable("marshmallow");
        controller.addVariable("lollipop");
        controller.addVariable("kitkat");
        controller.addVariable("android");
    }





    public interface CmdExecResult{
        void onCmdComplete(boolean success, int exitVal, String error, String output, String originalCmd);
    }


    public static String execCmdSync(String cmd, CmdExecResult callback) throws java.io.IOException, InterruptedException {
        Log.d("HTTP", "Running command: " + cmd);

        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(cmd);

        //String[] commands = {"system.exe","-get t"};

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

        StringBuffer stdout = new StringBuffer();
        StringBuffer errout = new StringBuffer();

        // read the output from the command
        System.out.println("Here is the standard output of the command:\n");
        String s = null;
        while ((s = stdInput.readLine()) != null) {
            System.out.println(s);
            stdout.append(s);
        }

        // read any errors from the attempted command
        System.out.println("Here is the standard error of the command (if any):\n");
        while ((s = stdError.readLine()) != null) {
            System.out.println(s);
            errout.append(s);
        }

        if (callback == null) {
            return stdInput.toString();
        }

        int exitVal = proc.waitFor();

        Log.d("HTTP", errout.toString());
        Log.d("HTTP", stdout.toString());

        callback.onCmdComplete(exitVal == 0, exitVal, errout.toString(), stdout.toString(), cmd);



        return stdInput.toString();
    }


    public InetAddress getWifiApIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en
                    .hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                if (intf.getName().contains("wlan")) {
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr
                            .hasMoreElements();) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress()
                                && (inetAddress.getAddress().length == 4)) {
                            Log.d("HTTP", inetAddress.getHostAddress());

                            return inetAddress;
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("HTTP", ex.toString());
        }
        return null;
    }

    //192.168.43.255

   /* InetAddress getBroadcastAddress() throws IOException {
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        // handle null somehow

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }*/


    class makeCompleteCallbackClass implements CmdExecResult {

        public void onCmdComplete(boolean success, int exitVal, String error, String output, String originalCmd){
            Log.d("HTTP", "make complete");


           runOnUiThread(new Runnable() {
                public void run() {

                   CheckBox codeGenComplete = (CheckBox)  complettionDialog.findViewById(R.id.compilingCheckBox);
                    codeGenComplete.setChecked(true);

               }
            });



            Log.d("HTTP", error);
            Log.d("HTTP", output);
        }
    }

    class gccCompleteCallbackClass implements CmdExecResult {

        public void onCmdComplete(boolean success, int exitVal, String error, String output, String originalCmd){
            Log.d("HTTP", "gcc complete");
        }
    }



    public void initOTA() {
       OTAtimeOutRetryCounter = 0;
        OTAstate = 0;
        if(timeoutOTA != null){
            timeoutOTA.removeCallbacksAndMessages(null);
        }


        if(timeoutUDP != null){
            timeoutUDP.removeCallbacksAndMessages(null);
        }
        startOTA();


    }



    private void startOTA(){
       // OTAstate = 0;
        Log.d("HTTP", "Start broadcast !!!!!");
        sendBroadcast_k("rom\n");




        timeoutOTA.postDelayed(
                new Runnable() {
                    public void run() {
                        Log.i("HTTP", "This'll run 300 milliseconds later");

                        OTAtimeOutRetryCounter++;
                        if(OTAtimeOutRetryCounter < 5){
                            startOTA();
                        } else {

                            // Report to user ...........
                            if(timeoutOTA != null){
                                timeoutOTA.removeCallbacksAndMessages(null);
                            }
                            OTAtimeOutRetryCounter = 0;
                        }

                    }
                }, ttout);
    }




    public void sendBroadcast_k(String messageStr) {
        Log.d("HTTP","Send " + messageStr);
        // Hack Prevent crash (sending should be done using an async task)
        StrictMode.ThreadPolicy policy = new   StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {
            InetAddress bbAddress = InetAddress.getByName(BROADCAST_ADDRESS);
            //InetAddress bbAddress = InetAddress.getByName("192.168.1.208");
            //Open a random port to send the package
            DatagramSocket socket = new DatagramSocket();
            socket.setBroadcast(true);
            byte[] sendData = messageStr.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, bbAddress, UDP_SERVER_PORT);
            socket.send(sendPacket);
            Log.d("HTTP",getClass().getName() + "Broadcast packet sent to: " + bbAddress.getHostAddress());


        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            Log.e("HTTP", "IOException: " + e.getMessage());
        }


    }


    private class RunServerInThread extends Thread{
        private boolean keepRunning = true;
        private String lastmessage = "";

        @Override
        public void run() {
            String message;
            DatagramSocket socket = null;


            try {

                socket = new DatagramSocket(UDP_SERVER_PORT);

                while(keepRunning) {

                    byte[] lmessage = new byte[MAX_UDP_DATAGRAM_LEN];
                    DatagramPacket packet = new DatagramPacket(lmessage, lmessage.length);



                    socket.receive(packet);
                    InetAddress destinationIP = packet.getAddress();


                    message = new String(lmessage, 0, packet.getLength());
                    message = message.trim();

                    packet.setLength(lmessage.length);



                 /*   if(InetAddressCompare(getWifiApIpAddress(),destinationIP) != 0) {
                        boardIP = destinationIP;
                        String tmm = message.toString().trim();
                        char romNum = tmm.charAt(tmm.length()-1);

                        Log.d("HTTP", "received message --------> " + boardIP.toString() + " rom =  " + romNum);






                    }*/




                    if(InetAddressCompare(getWifiApIpAddress(),destinationIP) != 0){

                        Log.d("HTTP", "Address = " + destinationIP.toString()+ " UDP = " + message);

                        //STATE MACHINE

                       // String tMessage = message.trim();
                        char sMessage = message.charAt(message.length() - 1);

                        switch(OTAstate) {
                            case 0:

                                if(sMessage == '0') {

                                    OTAstate = 1;

                                    //clear timeout

                                    if(timeoutOTA != null){
                                        timeoutOTA.removeCallbacksAndMessages(null);
                                    }

                                    UDPtimeOutRetryCounter = 0;

                                    sendUDPtimeout("switch",destinationIP, 100);


                                } else if(sMessage == '1') {
                                    OTAstate = 2;

                                    UDPtimeOutRetryCounter = 0;
                                    //clear timeout

                                    if(timeoutOTA != null){
                                        timeoutOTA.removeCallbacksAndMessages(null);
                                    }

                                    if(timeoutUDP != null){
                                        timeoutUDP.removeCallbacksAndMessages(null);
                                        sendUDPtimeout("ota=8000",destinationIP, 100);


                                        runOnUiThread(new Runnable() {
                                            public void run() {

                                                CheckBox codeGenComplete = (CheckBox)  complettionDialog.findViewById(R.id.connectingCheckBox);
                                                codeGenComplete.setChecked(true);
                                                codeGenComplete.setText("Connected");


                                            }
                                        });
                                    }
                                } else {
                                    Log.d("HTTP","ROM ERROR");
                                }
                                break;
                            case 1:
                                OTAstate = 0;
                                Log.d("HTTP", "In state 1");

                                UDPtimeOutRetryCounter = 0;
                                //clear timeout
                                if(timeoutUDP != null){
                                    timeoutUDP.removeCallbacksAndMessages(null);
                                    sendUDPtimeout("rom",destinationIP, 500);
                                }

                                break;
                            case 2:
                                //clear timeout
                                Log.d("HTTP", "In state 2");
                                UDPtimeOutRetryCounter = 0;
                                if(timeoutUDP != null){
                                    timeoutUDP.removeCallbacksAndMessages(null);
                                }

                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        //complettionDialog.dismiss();
                                        CheckBox codeGenComplete = (CheckBox)  complettionDialog.findViewById(R.id.uploadingCheckBox);
                                        codeGenComplete.setChecked(true);
                                        codeGenComplete.setText("Upload complete");


                                        complettionDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);

                                    }
                                });
                                break;
                            default:

                        }
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
            }  catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    socket.close();
                }
            }


        }

        public String getLastMessage() {
            return lastmessage;
        }
    }



        private int unsignedByteToInt(byte b) {
            return (int) b & 0xFF;
        }

        public int InetAddressCompare(InetAddress adr1, InetAddress adr2) {
            byte[] ba1 = adr1.getAddress();
            byte[] ba2 = adr2.getAddress();

            // general ordering: ipv4 before ipv6
            if(ba1.length < ba2.length) return -1;
            if(ba1.length > ba2.length) return 1;

            // we have 2 ips of the same type, so we have to compare each byte
            for(int i = 0; i < ba1.length; i++) {
                int b1 = unsignedByteToInt(ba1[i]);
                int b2 = unsignedByteToInt(ba2[i]);
                if(b1 == b2)
                    continue;
                if(b1 < b2)
                    return -1;
                else
                    return 1;
            }
            return 0;
        }



    private void sendUDPtimeout(final String command, final InetAddress ad, final long udpDelay){



        if(UDPDelayTimer != null) {
            UDPDelayTimer.removeCallbacks(null);
        }



        UDPDelayTimer.postDelayed(
                new Runnable() {
                    public void run() {
                        Log.d("HTTP", "UDP send " + command);
                        sendUDP(command + "\n", ad);


                        timeoutUDP.postDelayed(
                                new Runnable() {
                                    public void run() {
                                        Log.i("HTTP", "UDP This'll run 300 milliseconds later");

                                        UDPtimeOutRetryCounter++;
                                        if(UDPtimeOutRetryCounter < 2) {
                                            sendUDPtimeout(command, ad, udpDelay);
                                        }

                                    }
                                },
                                ttout);
                    }
                },
                udpDelay
        );



    }


    public void sendUDP(String messageStr, InetAddress add) {
        // Hack Prevent crash (sending should be done using an async task)
        StrictMode.ThreadPolicy policy = new   StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {
            //Open a random port to send the package
            DatagramSocket socket = new DatagramSocket();
            byte[] sendData = messageStr.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, add, UDP_SERVER_PORT);
            socket.send(sendPacket);
        } catch (IOException e) {
            Log.e("HTTP", "IOException: " + e.getMessage());
        }
    }



    public void showComplettionDiaglog(){


        runOnUiThread(new Runnable() {
            public void run() {
                //something here


                AlertDialog.Builder alert = new AlertDialog.Builder(TurtleActivity.this);
                alert.setTitle("Login");
                // this is set the view from XML inside AlertDialog
               // alert.setView(alertLayout);
                // disallow cancel of AlertDialog on click of back button and outside touch
                alert.setCancelable(false);
                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(getBaseContext(), "Cancel clicked", Toast.LENGTH_SHORT).show();
                    }
                });

                alert.setPositiveButton("Login", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                      /*  String user = etUsername.getText().toString();
                        String pass = etPassword.getText().toString();
                        Toast.makeText(getBaseContext(), "Username: " + user + " Password: " + pass, Toast.LENGTH_SHORT).show();*/
                    }
                });
                complettionDialog = alert.create();
                complettionDialog.show();


            }
        });


    }


    public static class WiFiConnectionReciever extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.net.wifi.WIFI_AP_STATE_CHANGED".equals(action)) {

                // get Wi-Fi Hotspot state here
                int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);

                if (WifiManager.WIFI_STATE_ENABLED == state % 10) {
                    // Wifi is enabled
                }

            }
        }
    }


    public class copyAssetTask extends AsyncTask<Void,Long,Boolean> {
        ProgressDialog progress;

        @Override
        protected Boolean doInBackground(Void... voids) {

            copyFilesToSdCard();

            return null;
        }

        @Override
        protected void onPreExecute() {
            runOnUiThread(new Runnable() {
                public void run() {

                    progress = ProgressDialog.show(TurtleActivity.this, "", "Copying files for the first run. It may take several minutes.", true);
                }
            });
        }


        @Override
        protected void onPostExecute(Boolean success) {
            runOnUiThread(new Runnable() {
                public void run() {

                    progress.dismiss();

                }
            });


            // Show dialog with result
        }

        @Override
        protected void onProgressUpdate(Long... values) {
           // progress.setMessage("Transferred " + values[0] + " bytes");
        }
    }


    private void copyFilesToSdCard() {
        copyFileOrDir(""); // copy all files in assets folder in my project
    }

    private void copyFileOrDir(String path) {
        AssetManager assetManager = this.getAssets();
        String assets[] = null;
        try {
            //  Log.i("tag", "copyFileOrDir() "+path);
            assets = assetManager.list(path);
            if (assets.length == 0) {
                copyFile(path);
            } else {
                String fullPath =  assetPath + path;
                Log.i("tag", "path="+fullPath);
                File dir = new File(fullPath);
                if (!dir.exists() && !path.startsWith("images") && !path.startsWith("sounds") && !path.startsWith("webkit"))
                    if (!dir.mkdirs())
                        Log.i("tag", "could not create dir "+fullPath);
                for (int i = 0; i < assets.length; ++i) {
                    String p;
                    if (path.equals(""))
                        p = "";
                    else
                        p = path + "/";

                    if (!path.startsWith("images") && !path.startsWith("sounds") && !path.startsWith("webkit"))
                        copyFileOrDir( p + assets[i]);

                    double pgress = 1.0*i/assets.length;

                    Log.d("HTTP", "Percent = " + Double.toString(pgress));

                }
            }
        } catch (IOException ex) {
            Log.e("tag", "I/O Exception", ex);
        }
    }

    private void copyFile(String filename) {
        AssetManager assetManager = this.getAssets();

        InputStream in = null;
        OutputStream out = null;
        String newFileName = null;
        try {
            Log.i("tag", "copyFile() "+filename);
            in = assetManager.open(filename);
            if (filename.endsWith(".jpg")) // extension was added to avoid compression on APK file
                newFileName = assetPath + filename.substring(0, filename.length()-4);
            else
                newFileName = assetPath + filename;
            out = new FileOutputStream(newFileName);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;
            out.flush();

            out.close();
            out = null;

            File mypath=new File(assetPath, filename);
            mypath.setExecutable(true);




        } catch (Exception e) {
            Log.e("tag", "Exception in copyFile() of "+newFileName);
            Log.e("tag", "Exception in copyFile() "+e.toString());
        }

    }



    private boolean checkSystemWritePermission() {
        boolean retVal = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            retVal = Settings.System.canWrite(this);
            Log.d(TAG, "Can Write Settings: " + retVal);
            if(retVal){
                //Toast.makeText(this, "Write allowed :-)", Toast.LENGTH_LONG).show();
            }else{
                //Toast.makeText(this, "Write not allowed :-(", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" +  getPackageName()));
                startActivity(intent);
            }
        }
        return retVal;
    }



}
