package cyq.com.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.ParcelUuid;
import android.widget.Toast;

import java.io.IOException;
import java.util.*;

public class AcceptThread extends Thread {
    private Handler handler;
    private Context context;
    private BluetoothServerSocket serverSocket;

    public AcceptThread( BluetoothAdapter bluetoothAdapter,Context context,Handler handler){
        this.context = context;
        this.handler = handler;
        try {
            String name = "c-y-q-com-bluetooth";
            UUID uuid = UUID.randomUUID();
            serverSocket =  bluetoothAdapter.listenUsingRfcommWithServiceRecord(name,uuid);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        super.run();
        try {
            BluetoothSocket bluetoothSocket = serverSocket.accept();

            if (bluetoothSocket!=null){//已连接
                serverSocket.close();
                Toast.makeText(context,"socket已连接",Toast.LENGTH_SHORT).show();

                StreamThread streamThread = new StreamThread(bluetoothSocket,handler);
                streamThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /** Will cancel the listening socket, and cause the thread to finish */
    public void cancel(){
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
