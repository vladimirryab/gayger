package com.example.gayger;

// TODO последняя версия приложения от 12/05/2022 0:31. Да поможет нам Бог.

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    public ConnectThread connectThread;
    public ConnectedThread connectedThread;

    public boolean Get = false;
    public boolean isConnected = false;
    public static final int BT_BOUNDED = 21;
    public static final int BT_SEARCH = 22;

    public static final String GAYGER = "GAYGER";
    private BluetoothAdapter btAdapter;

    public TextView bluetooth;
    public ImageButton radbutton;
    public ImageView radicon;
    public Button button;
    public BtListAdapter listAdapter;
    public FrameLayout listlayout;
    public ListView listBtDevices;
    public TextView listtext;
    public ImageView caution;
    public TextView result;

    private ArrayList<BluetoothDevice> bluetoothDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothDevices = new ArrayList<>();
        listlayout = findViewById(R.id.listlayout);
        result = findViewById(R.id.result);
        bluetooth = findViewById(R.id.bluetooth);
        radbutton = findViewById(R.id.radbutton);
        radbutton.setOnClickListener(clickListener);
        radicon = findViewById(R.id.imageView);
        caution = findViewById(R.id.caution);
        button = findViewById(R.id.button);
        button.setOnClickListener(clickListener);
        listBtDevices   = findViewById(R.id.lv_bt_device);
        listBtDevices.setOnItemClickListener(itemClickListener);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        listtext = findViewById(R.id.listtext);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(receiver, filter);

        StatusBT();
    }
    private void setListAdapter(int type) {

        bluetoothDevices.clear();
        int iconType = R.drawable.ic_bluetooth_bounded_device;

        switch (type) {
            case BT_BOUNDED:
                bluetoothDevices = getBoundedBtDevices();
                iconType = R.drawable.ic_bluetooth_bounded_device;
                break;
            case BT_SEARCH:
                iconType = R.drawable.ic_bluetooth_search_device;
                break;
        }
        listAdapter = new BtListAdapter(this, bluetoothDevices, iconType);
        listBtDevices.setAdapter(listAdapter);
    }

    @SuppressLint("MissingPermission")
    private ArrayList<BluetoothDevice> getBoundedBtDevices() {
        Set<BluetoothDevice> deviceSet = btAdapter.getBondedDevices();
        ArrayList<BluetoothDevice> tmpArrayList = new ArrayList<>();
        if (deviceSet.size() > 0) {
            for (BluetoothDevice device: deviceSet) {
                if (device.getName().equals(GAYGER)) {
                    tmpArrayList.add(device);
                }
            }
        }
        return tmpArrayList;
    }

    View.OnClickListener clickListener = new View.OnClickListener() {

        @SuppressLint("MissingPermission")
        @Override
        public void onClick(View view) {
            if (view.equals(button)) {
                if (!btAdapter.isEnabled()) {
                    EnableBT();
                }
                else if (Get) {
                    Toast.makeText(getApplicationContext(), "Пожалуйста, дождитесь окончания замера перед отключением от устройства...", Toast.LENGTH_SHORT).show();
                }
                else if (btAdapter.isEnabled() && !isConnected) {
                    SearchBT();
                }
                else if ((connectedThread != null || connectThread != null) && !Get)
                // TODO отключение от устройства
                    cancel();
            }
            else if (view.equals(radbutton)) {
                if (Get) {
                    Toast.makeText(getApplicationContext(), "Производится замер, пожалуйста, подождите...", Toast.LENGTH_SHORT).show();
                }
                else if (connectedThread != null && connectThread != null && isConnected) {
                    Toast.makeText(getApplicationContext(),"Отправляем запрос на получение концентрации...",Toast.LENGTH_SHORT).show();
                    connectedThread.write("GET#");
                    Get = true;
                }
            }
        }
    };
    public void onConnected(String name) {
        isConnected = true;
        button.setText("ОТКЛЮЧИТЬСЯ");
        radicon.setVisibility(View.INVISIBLE);
        radbutton.setVisibility(View.VISIBLE);
        Toast.makeText(MainActivity.this, "Подключено к устройству: " + name, Toast.LENGTH_SHORT).show();
        listlayout.setVisibility(View.GONE);
        listtext.setText("Показания счётчика:");
        caution.setColorFilter(Color.GREEN);
        caution.setVisibility(View.VISIBLE);
        result.setText("0 БК/м³");
        result.setVisibility(View.VISIBLE);
    }
    AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @SuppressLint("MissingPermission")
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            if (adapterView.equals(listBtDevices)) {
                BluetoothDevice device = bluetoothDevices.get(i);
                if(device != null) {
                    connectThread = new ConnectThread(device);
                    connectThread.start();
                    if (btAdapter.isDiscovering()) {
                        btAdapter.cancelDiscovery();
                    }
                }
            }
        }
    };

    @SuppressLint("MissingPermission")
    public void SearchBT() {
        if (btAdapter.isDiscovering()) {
            btAdapter.cancelDiscovery();
        } else {
            accessLocationPermission();
            btAdapter.startDiscovery();
        }
    }
    public void accessLocationPermission() {
        int accessCoarseLocation = this.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION);
        int accessFineLocation = this.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION);

        List<String> listRequestPermission = new ArrayList<>();

        if (accessCoarseLocation != PackageManager.PERMISSION_GRANTED) {
            listRequestPermission.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (accessFineLocation != PackageManager.PERMISSION_GRANTED) {
            listRequestPermission.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (!listRequestPermission.isEmpty()) {
            String[] strRequestPermission = listRequestPermission.toArray(new String[listRequestPermission.size()]);
            this.requestPermissions(strRequestPermission, 2);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 2:
                if (grantResults.length > 0) {
                    for (int gr : grantResults) {
                        if (gr != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                    }
                    SearchBT();
                }
                break;
            default:
                return;
        }
    }
    public void cancel() {
        if (connectedThread != null) {
            connectedThread.cancelConnectedThread();
            isConnected = false;
        }
        else if (connectThread != null) {
            connectThread.cancelConnectThread();
            isConnected = false;
        }
        Toast.makeText(getApplicationContext(),"Отключение от устройства...",Toast.LENGTH_SHORT).show();
        Get = false;
        caution.setVisibility(View.GONE);
        result.setVisibility(View.GONE);
        listtext.setVisibility(View.VISIBLE);
        listtext.setText(R.string.founded);
        listlayout.setVisibility(View.VISIBLE);
        radbutton.setVisibility(View.GONE);
        radicon.setVisibility(View.VISIBLE);
        button.setText(R.string.start_search);
    }
    public void StatusBT() {
        if (btAdapter.isEnabled()) {
            bluetooth.setTextColor(Color.GREEN);
            bluetooth.setText("Bluetooth подключен");
            setListAdapter(BT_BOUNDED);
            button.setText(R.string.start_search);
            listtext.setVisibility(View.VISIBLE);
            listlayout.setVisibility(View.VISIBLE);

        } else if (!btAdapter.isEnabled()) {
            isConnected = false; Get = false;
            result.setVisibility(View.INVISIBLE);
            radicon.setVisibility(View.VISIBLE);
            caution.setVisibility(View.INVISIBLE);
            radbutton.setVisibility(View.INVISIBLE);
            bluetooth.setTextColor(Color.RED);
            bluetooth.setText("Bluetooth не подключен");
            button.setText("НАЧАТЬ");
            listlayout.setVisibility(View.GONE);
            listtext.setVisibility(View.GONE);
        }
    }

    @SuppressLint("MissingPermission")
    public void EnableBT() {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_CANCELED) {
                EnableBT();
            }
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            switch (action) {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    StatusBT();
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    button.setText(R.string.stop_search);
                    setListAdapter(BT_SEARCH);
                    listtext.setText(R.string.founded);
                    Toast.makeText(getApplicationContext(), "Начало поиска...", Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    button.setText(R.string.start_search);
                    Toast.makeText(getApplicationContext(), "Конец поиска...", Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device != null) {
                        if (device.getName() != null && device.getName().equals(GAYGER)) {
                            try {
                                bluetoothDevices.add(device);
                                listAdapter.notifyDataSetChanged();
                                SearchBT();
                                break;
                            } catch (Throwable e) {
                                Toast.makeText(getApplicationContext(), "Ошибка при поиске...", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                    break;
            }
        }
    };
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        cancel();
    }

    private class ConnectThread extends Thread {

        private BluetoothSocket bluetoothSocket = null;
        private boolean success = false;
        public String name;

        @SuppressLint("MissingPermission")
        public ConnectThread(BluetoothDevice device) {
            name = device.getName();
            try {
                Method method = device.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                bluetoothSocket = (BluetoothSocket) method.invoke(device, 1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        @SuppressLint("MissingPermission")
        @Override
        public void run() {
            try {
                bluetoothSocket.connect();
                success = true;
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Не удалось подключиться...", Toast.LENGTH_SHORT).show());
            }

            if (success) {
                connectedThread = new ConnectedThread(bluetoothSocket);
                connectedThread.start();
                runOnUiThread(() -> onConnected(name));
            }
        }
        public void cancelConnectThread() {
            try {
                bluetoothSocket.close();
                isConnected = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private class ConnectedThread extends Thread {

        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket bluetoothSocket) {
            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                inputStream = bluetoothSocket.getInputStream();
                outputStream = bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            this.inputStream = inputStream;
            this.outputStream = outputStream;
        }
        @Override
        public void run() {
            BufferedInputStream bis = new BufferedInputStream(inputStream);
            StringBuffer buffer = new StringBuffer();
            StringBuffer input;
            while(isConnected) {
                try {
                    int bytes = bis.read();
                    buffer.append((char) bytes);
                    int eof = buffer.indexOf("\r\n");
                    if (eof > 0) {
                        input = buffer;
                        String res = input.delete(input.length()-2,input.length()).toString();
                        buffer.delete(0,buffer.length());

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                int resulted = 0;
                                try {
                                    resulted = Integer.parseInt(res);
                                } catch (NumberFormatException nfe) {
                                    Toast.makeText(getApplicationContext(),"Error: "+nfe,Toast.LENGTH_SHORT).show();
                                }
                                Get = false;
                                result.setText(resulted + " БК/м³");
                                if (resulted < 200) {
                                    caution.setColorFilter(Color.GREEN);
                                } else if (resulted < 2000) {
                                    caution.setColorFilter(Color.YELLOW);
                                } else {
                                    caution.setColorFilter(Color.RED);
                                }
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                bis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void write(String command) {
            byte[] bytes = command.getBytes();
            if (outputStream != null) {
                try {
                    outputStream.write(bytes);
                    outputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void cancelConnectedThread() {
            try {
                inputStream.close();
                outputStream.close();
                isConnected = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}