package cyq.com.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamThread extends Thread {
    private Handler handler;
    BluetoothSocket socket;
    InputStream inputStream;
    OutputStream outputStream;
    public StreamThread(BluetoothSocket socket,Handler handler){
        this.handler = handler;
        this.socket = socket;
        try {
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        super.run();

            byte[] buffer = new byte[1024];
            int byteNum;
            while(true){
                try {
                    byteNum = inputStream.read(buffer);
                    //将读取的信息发送主界面处理
                    handler.obtainMessage(1,byteNum,-1,buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
    }

    public void write(byte[] bytes){
        try {
            outputStream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* Call this from the main activity to shutdown the connection */
    public void cancel(){
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
