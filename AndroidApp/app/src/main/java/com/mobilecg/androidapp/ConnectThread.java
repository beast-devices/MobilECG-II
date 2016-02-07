package com.mobilecg.androidapp;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

class ConnectThread extends Thread {
    private BluetoothSocket mmSocket;
    private BluetoothDevice mmDevice;
    private BluetoothAdapter mBluetoothAdapter;
    private InputStream mmInStream;
    private OutputStream mmOutStream;
    private boolean mIsConnected;

    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public ConnectThread() {
        mIsConnected=false;
    }

    public void connect(String macaddr){
        if (mIsConnected)
            return;

        if(isAlive()){
            interrupt();
        }

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mmDevice = mBluetoothAdapter.getRemoteDevice(macaddr);

        try {
            // MY_UUID is the app's UUID string, also used by the server code
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            return;
        }


        start();
    }

    public void disconnect(){
        if (!mIsConnected)
            return;

        cancel();
    }

    public void run() {
        // Cancel discovery because it will slow down the connection
        mBluetoothAdapter.cancelDiscovery();

        try {
            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            mmSocket.connect();
            mmInStream = mmSocket.getInputStream();
            mmOutStream = mmSocket.getOutputStream();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and get out
            try {
                mmSocket.close();
            } catch (IOException closeException) { }

            return;
        }

        byte[] buffer = new byte[1024];  // buffer store for the stream
        int bytes; // bytes returned from read()

        mIsConnected=true;
        EcgJNI.onDeviceConnected();

        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                // Read from the InputStream
                bytes = mmInStream.read(buffer);
                // Send the obtained bytes to the UI activity
                EcgJNI.processEcgData(buffer, bytes);
            } catch (IOException e) {
                break;
            }
        }

        mIsConnected=false;
        EcgJNI.onDeviceDisconnected();

    }

    public boolean isConnected(){
        return mIsConnected;
    }

    /** Will cancel an in-progress connection, and close the socket */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }

    /* Call this from the main activity to send data to the remote device */
    public void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);
        } catch (IOException e) { }
    }

}