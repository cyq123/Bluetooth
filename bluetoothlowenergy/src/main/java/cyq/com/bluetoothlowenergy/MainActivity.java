package cyq.com.bluetoothlowenergy;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 低功耗蓝牙Android 4.3 (API level 18)
 */
public class MainActivity extends AppCompatActivity {
    private Context context;
    private final int REQUEST_CODE = 1;
    private int scan_period = 12000;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice cdevice;
    private BluetoothGatt bluetoothGatt;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        //检查设备是否支持ble功能，只在<uses-feature.../>设为false时需要检查
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
            Toast.makeText(this, "设备不支持低功耗蓝牙", Toast.LENGTH_SHORT).show();
            return;
        }

        //1.获取蓝牙适配器
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        //2.检查蓝牙是否启用
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()){
            bluetoothAdapter.enable();//用这个方法会提示：应用名+需要打开蓝牙
            /*Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent,REQUEST_CODE);//提示：某个应用需要打开蓝牙*/
        }

        //3.扫描ble蓝牙设备
        scanBleDevice();

        //4.用扫描到的设备，连接到GATT服务
        if (cdevice!=null){
            bluetoothGatt=cdevice.connectGatt(context,false,gattCallback);
        }
    }

    private void scanBleDevice() {
        final BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();//5.0新增扫描方法
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                bluetoothLeScanner.stopScan(scanCallback);//开启扫描10秒后停止扫描
            }
        }, scan_period);

        bluetoothLeScanner.startScan(scanCallback);
    }

    ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            cdevice = result.getDevice();
            Toast.makeText(context, "扫描到的ble设备:"+cdevice.getName(), Toast.LENGTH_SHORT).show();
        }
    };

    BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override//1.
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED){
                //服务连接后在这里启动发现服务
                bluetoothGatt.discoverServices();//该方法会回调onServicesDiscovered（）
                Toast.makeText(context, "gatt服务已连接", Toast.LENGTH_SHORT).show();
            }else if (newState == BluetoothProfile.STATE_DISCONNECTED){
                Toast.makeText(context, "gatt服务连接已断开", Toast.LENGTH_SHORT).show();
            }
        }

        @Override//2.
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            if (status == BluetoothGatt.GATT_SUCCESS){
                List<BluetoothGattService> services = bluetoothGatt.getServices();//发现服务后，获取服务列表
                BluetoothGattService bluetoothGattService = services.get(0);//获取指定的服务
                List<BluetoothGattCharacteristic> characteristics = bluetoothGattService.getCharacteristics();//获取特征列表
                BluetoothGattCharacteristic bluetoothGattCharacteristic =  characteristics.get(0);//获取指定的特征

                //订阅特征变化的通知
                /**
                 * 调用 bluetoothGatt.setCharacteristicNotification() 方法，传入一个特征 bluetoothGattCharacteristic 对象。
                 * 当这个特征里的数据发生变化（接收到数据了），会触发 回调方法的 onCharacteristicChanged 方法。我们在这个回调方法中读取数据。
                 *
                 */
                bluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic,true);
                UUID uuid = bluetoothGattCharacteristic.getUuid();
                BluetoothGattDescriptor bluetoothGattDescriptor = bluetoothGattCharacteristic.getDescriptor(uuid);
                bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                bluetoothGatt.writeDescriptor(bluetoothGattDescriptor);
                Toast.makeText(context, "gatt发现新服务", Toast.LENGTH_SHORT).show();
            }

        }

        @Override//3.
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            bluetoothGatt.readCharacteristic(characteristic);//读取数据
            byte[] value = characteristic.getValue();
            String str = new String(value);
            Toast.makeText(context, "收到的信息:"+str, Toast.LENGTH_SHORT).show();
        }

        @Override
        // Result of a characteristic read operation
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Toast.makeText(context, "特征:"+characteristic.getUuid(), Toast.LENGTH_SHORT).show();
        }
    };

    /**
     * 写入数据时，我们需要先获得特征，特征存在于服务内，一般在发现服务的 onServicesDiscovered 时，查找到特征对象
     * @param bytes
     * @param characteristic
     */
    private void write(byte[] bytes,BluetoothGattCharacteristic characteristic) {
        characteristic.setValue(bytes);//单次最多20个字节
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        bluetoothGatt.writeCharacteristic(characteristic);
    }

    Handler handler = new Handler(){

    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case REQUEST_CODE://请求开启蓝牙
                Toast.makeText(this, "蓝牙已开启", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
