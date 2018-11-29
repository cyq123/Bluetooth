package cyq.com.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.ParcelUuid;

import java.io.IOException;
import java.util.UUID;

public class ConnectThread extends Thread {
    private BluetoothSocket socket;
    private Handler handler;
    public ConnectThread(BluetoothDevice device,Handler handler){
        this.handler = handler;
        try {
            ParcelUuid[] parcelUuid = device.getUuids();
            socket = device.createRfcommSocketToServiceRecord(parcelUuid[0].getUuid());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        super.run();
        try {
            socket.connect();//阻塞调用，建议别放在主线程

            StreamThread streamThread = new StreamThread(socket,handler);
            streamThread.start();
        } catch (IOException e) {
            try {
                socket.close();
            } catch (IOException e1) {
            }
            e.printStackTrace();
        }
    }
    /** Will cancel an in-progress connection, and close the socket */
    public void cancel(){
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
