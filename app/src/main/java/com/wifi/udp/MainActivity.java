package com.wifi.udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Objects;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    static final String LOG_TAG = "UDPchat";
    private static final int LISTENER_PORT = 50003;
    private static final int BUF_SIZE = 1024;
    private ContactManager contactManager;
    private String displayName;
    private boolean STARTED = false;
    private boolean IN_CALL = false;
    private boolean LISTEN = false;

    private Handler handler = new Handler();
    private EditText editIpAddress;
    private EditText editPort;

    private String ipAddress;
    private String port;
    private TextView ipText;

    public final static String EXTRA_CONTACT = "hw.dt83.udpchat.CONTACT";
    public final static String EXTRA_IP = "hw.dt83.udpchat.IP";
    public final static String EXTRA_DISPLAYNAME = "hw.dt83.udpchat.DISPLAYNAME";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i(LOG_TAG, "UDPChat started");

        // START BUTTON
        // Pressing this buttons initiates the main functionality
        editIpAddress = findViewById(R.id.editIpAddress);
        editPort = findViewById(R.id.editPort);
        ipText = findViewById(R.id.ipText);
        final Button btnStart = (Button) findViewById(R.id.buttonStart);

        ipText.setText(Objects.requireNonNull(getCurrentIp()).getHostAddress());
        btnStart.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                 ipAddress = editIpAddress.getText().toString();
                 port = editPort.getText().toString();

                if(ipAddress.equals("")){
                    Toast.makeText(MainActivity.this,"Ip Address cannot be empty",Toast.LENGTH_LONG).show();
                    return;
                }
                if(port.equals("")){
                    Toast.makeText(MainActivity.this,"Port cannot be empty",Toast.LENGTH_LONG).show();
                    return;
                }

                Log.i(LOG_TAG, "Start button pressed");
                STARTED = true;

                displayName = Build.MODEL;

                btnStart.setEnabled(false);

                Log.d("UDP TAG","Ip Address " + formattedIp(ipAddress));
                Log.i("UDP TAG","Port " + Integer.parseInt(port));
                contactManager = new ContactManager(displayName,formattedIp(ipAddress),Integer.parseInt(port));
                startCallListener();
                listenToContact();
            }
        });

    }

    private void listenToContact(){

         Runnable checkRadioGroupRunnable = new Runnable() {
            @Override
            public void run() {

                if (!contactManager.getContacts().isEmpty() && contactManager.getContacts() != null){
                    handler.removeCallbacksAndMessages("");
                    InetAddress ip = contactManager.getContacts().get(0).getInetAddress();
                    String contact = contactManager.getContacts().get(0).getContact();
                    IN_CALL = true;

                    try {
                        // Send this information to the MakeCallActivity and start that activity
                        Intent intent = new Intent(MainActivity.this, MakeCallActivity.class);
                        intent.putExtra(EXTRA_CONTACT, contact);
                        String address = ip.toString();
                        address = address.substring(1);
                        intent.putExtra(EXTRA_IP, address);
                        intent.putExtra(EXTRA_DISPLAYNAME, displayName);
                        startActivity(intent);
                    } catch (Exception e) {
                        Log.d("TAG", "Crash " + e.getMessage());
                    }
                } else {
                    // No contact selected, post the Runnable again with a delay
                    handler.postDelayed(this, 1000);
                    Toast.makeText(MainActivity.this,"Waiting for receiver",Toast.LENGTH_LONG).show();
                }
            }
        };
         handler.post(checkRadioGroupRunnable);
    }

    private InetAddress formattedIp(String userIp){
        try {
            String[] parts = userIp.split("\\.");

            int combined = (Integer.parseInt(parts[3]) << 24)
                    | (Integer.parseInt(parts[2]) << 16)
                    | (Integer.parseInt(parts[1]) << 8)
                    | Integer.parseInt(parts[0]);

            return InetAddress.getByName(Formatter.formatIpAddress(combined));
        } catch (UnknownHostException e){
            return null;
        }
    }
    private InetAddress getCurrentIp() {
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            int ip = wifiManager.getConnectionInfo().getIpAddress();
            String addressString = Formatter.formatIpAddress(ip);
            Log.d("UDP TAG","Ip Address2 " + addressString);
            return InetAddress.getByName(addressString);
        } catch (UnknownHostException e){
            return null;
        }
    }

    private String toBroadcastIp(int ip) {
        // Returns converts an IP address in int format to a formatted string
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                "255";
    }

    private void startCallListener() {
        // Creates the listener thread
        LISTEN = true;
        Thread listener = new Thread(new Runnable() {

            @Override
            public void run() {

                try {
                    // Set up the socket and packet to receive
                    Log.i(LOG_TAG, "Incoming call listener started");
                    DatagramSocket socket = new DatagramSocket(LISTENER_PORT);
                    socket.setSoTimeout(1000);
                    byte[] buffer = new byte[BUF_SIZE];
                    DatagramPacket packet = new DatagramPacket(buffer, BUF_SIZE);
                    while(LISTEN) {
                        // Listen for incoming call requests
                        try {
                            Log.i(LOG_TAG, "Listening for incoming calls");
                            socket.receive(packet);
                            String data = new String(buffer, 0, packet.getLength());
                            Log.i(LOG_TAG, "Packet received from "+ packet.getAddress() +" with contents: " + data);
                            String action = data.substring(0, 4);
                            if(action.equals("CAL:")) {
                                // Received a call request. Start the ReceiveCallActivity
                                String address = packet.getAddress().toString();
                                String name = data.substring(4, packet.getLength());

                                Intent intent = new Intent(MainActivity.this, ReceiveCallActivity.class);
                                intent.putExtra(EXTRA_CONTACT, name);
                                intent.putExtra(EXTRA_IP, address.substring(1, address.length()));
                                IN_CALL = true;
                                //LISTEN = false;
                                //stopCallListener();
                                startActivity(intent);
                            }
                            else {
                                // Received an invalid request
                                Log.w(LOG_TAG, packet.getAddress() + " sent invalid message: " + data);
                            }
                        }
                        catch(Exception e) {}
                    }
                    Log.i(LOG_TAG, "Call Listener ending");
                    socket.disconnect();
                    socket.close();
                }
                catch(SocketException e) {

                    Log.e(LOG_TAG, "SocketException in listener " + e);
                }
            }
        });
        listener.start();
    }

    private void stopCallListener() {
        // Ends the listener thread
        LISTEN = false;
    }

    @Override
    public void onPause() {

        super.onPause();
        if(STARTED) {

            contactManager.bye(displayName);
            contactManager.stopBroadcasting();
            contactManager.stopListening();
            //STARTED = false;
        }
        stopCallListener();
        Log.i(LOG_TAG, "App paused!");
    }

    @Override
    public void onStop() {

        super.onStop();
        Log.i(LOG_TAG, "App stopped!");
        stopCallListener();
        if(!IN_CALL) {

            finish();
        }
    }

    @Override
    public void onRestart() {

        super.onRestart();
        Log.i(LOG_TAG, "App restarted!");
        IN_CALL = false;
        STARTED = true;
        contactManager = new ContactManager(displayName, getCurrentIp(),Integer.parseInt(port));
        startCallListener();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages("");
        super.onDestroy();
    }
}