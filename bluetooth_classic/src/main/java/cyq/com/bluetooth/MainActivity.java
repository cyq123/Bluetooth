package cyq.com.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import cyq.com.bluetooth.adapter.EnableAdapter;
import cyq.com.bluetooth.adapter.PairedAdapter;
import cyq.com.bluetooth.vo.DeviceVO;
import cyq.com.bluetooth.widget.ListviewNoScoll;

public class MainActivity extends AppCompatActivity {
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private final int BLUE_REQUEST_CODE = 1;
    private final int REQUEST_DISCOVERABLE = 2;

    private ProgressBar progressBar;
    private FloatingActionButton floatingActionButton;
    private PairedAdapter pairedAdapter;
    private EnableAdapter enableAdapter;
    private ListviewNoScoll paired_device_listview;
    private ListviewNoScoll enable_device_listview;
    private List<BluetoothDevice> pairedList = new ArrayList<BluetoothDevice>();
    private List<BluetoothDevice> enableList = new ArrayList<BluetoothDevice>();
    private void initListview(){
        paired_device_listview = findViewById(R.id.paired_listview);
        pairedAdapter = new PairedAdapter(context,pairedList);
        paired_device_listview.setAdapter(pairedAdapter);

        enable_device_listview = findViewById(R.id.enable_listview);
        enableAdapter = new EnableAdapter(context,enableList);
        enable_device_listview.setAdapter(enableAdapter);
        enable_device_listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                bluetoothAdapter.cancelDiscovery();//调用连接之前，取消取消发现
                BluetoothDevice device = enableList.get(i);
                new ConnectThread(device,handler).start();
            }
        });
    }
    private void initFloatingActionButton(){
        floatingActionButton = findViewById(R.id.floatingActionButton);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                enableList.clear();
                enableAdapter.notifyDataSetChanged();
                discoverDevice();
            }
        });
    }
    private void initProgress(){
        progressBar = findViewById(R.id.progress_circular);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        initListview();
        initProgress();
        initFloatingActionButton();
       //1.获取BluetoothAdapter实例，判断设备是否支持蓝牙功能.
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
         if (bluetoothAdapter == null){
            Toast.makeText(this,"当前设备不支持蓝牙功能",Toast.LENGTH_SHORT).show();
            return;
        }
        //2.检查蓝牙是否开启，未开启则调用ACTION_REQUEST_ENABLE打开,开启后在onActivityResult中回调
        if (!bluetoothAdapter.isEnabled()){
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent,BLUE_REQUEST_CODE);
        }

        //(可选,若启用可检测性可忽略1和2两步)启用可检测性,将会自动启用蓝牙.使用 ACTION_REQUEST_DISCOVERABLE 操作 Intent
       /* Intent discoverAbleIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        startActivityForResult(discoverAbleIntent,REQUEST_DISCOVERABLE);//开启后在onActivityResult中回调*/

        //3.查询配对的设备.在执行设备发现之前，有必要查询已配对的设备集，以了解所需的设备是否处于已知状态
        Set<BluetoothDevice> blueList = bluetoothAdapter.getBondedDevices();
        if (blueList.size()>0){
           for (BluetoothDevice device : blueList){
               pairedList.add(device);
           }
            pairedAdapter.notifyDataSetChanged();
        }

        //4.注册广播：您的应用必须针对 ACTION_FOUND Intent 注册一个 BroadcastReceiver，以便接收每台发现的设备的相关信息
        discoverDevice();//This is an asynchronous call发现设备
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);//开始发现设备广播
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);//结束发现设备广播
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);//蓝牙已连接广播
        registerReceiver(blueReceiver,intentFilter);


        new AcceptThread(bluetoothAdapter,context,handler).start();
    }
    private Handler handler = new MyHandler();
    private static class MyHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK){
            switch (requestCode){
                case BLUE_REQUEST_CODE:
                    Toast.makeText(this, "蓝牙已开启", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
        if (resultCode == 120){
            if (requestCode == REQUEST_DISCOVERABLE)
                Toast.makeText(this, "蓝牙已开启可检测", Toast.LENGTH_SHORT).show();
        }
    }


    BroadcastReceiver blueReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //此 Intent 将携带额外字段 EXTRA_DEVICE 和 EXTRA_CLASS，二者分别包含 BluetoothDevice 和 BluetoothClass。
            String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_FOUND)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String name = device.getName();
                if (!TextUtils.isEmpty(name)){
                    enableList.add(device);
                    enableAdapter.notifyDataSetChanged();
                }
            }else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)){
                Toast.makeText(context,"扫描开始",Toast.LENGTH_LONG).show();
                progressBar.animate().alpha(1).setDuration(2000);
            }else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)){
                Toast.makeText(context,"扫描结束",Toast.LENGTH_LONG).show();
                progressBar.animate().alpha(0).setDuration(2000);
            }else if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)){
                Toast.makeText(context,"蓝牙已连接",Toast.LENGTH_LONG).show();
            }
        }
    };

    private void discoverDevice(){
        bluetoothAdapter.startDiscovery();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(blueReceiver);
    }
}
