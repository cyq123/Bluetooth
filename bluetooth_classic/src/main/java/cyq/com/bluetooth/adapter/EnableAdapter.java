package cyq.com.bluetooth.adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import cyq.com.bluetooth.R;
import cyq.com.bluetooth.vo.DeviceVO;

public class EnableAdapter extends BaseAdapter {
    private Context context;
    private List<BluetoothDevice> list;
    public EnableAdapter(Context context,List<BluetoothDevice> list){
        this.context = context;
        this.list = list;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int i) {
        return list.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null){
            view = LayoutInflater.from(context).inflate(R.layout.paired_list_item,null);
        }

        TextView info = view.findViewById(R.id.textView);
        ImageView img = view.findViewById(R.id.imageView);

        BluetoothDevice device = list.get(i);
        info.setText(device.getName()+"("+device.getAddress()+")");

        return view;
    }
}
