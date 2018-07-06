package edu.upc.appsensors;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.design.widget.NavigationView;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {
    private ServiceConnection mConnection;
    private IPCMain ipc;
    private NavigationView navigationView;


    static public MainStat st = new MainStat();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Context context = getBaseContext();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        ipc = new IPCMain(this);

        Intent intent = new Intent(context, MainService.class);

        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                ipc.setMClient(new Messenger(service));
                System.out.println("Set ipc client");

                ipc.send(AppMessages.register());
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                ipc.setMClient(null);
                System.out.println("unSet ipc client");
            }
        };

        context.startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        //Initialize interface

        if(st.log) {
            ((Button) findViewById(R.id.log_record)).setText("Stop recording Log");
        } else {
            ((Button) findViewById(R.id.log_record)).setText("Record Log");
        }
        if(st.clientSt) {
            ((Button) findViewById(R.id.client_start)).setText("Stop Client");
        } else {
            ((Button) findViewById(R.id.client_start)).setText("Start Client");
        }
        if(st.serverSt) {
            ((Button) findViewById(R.id.server_connect)).setText("Disconnect from Server");
        } else {
            ((Button) findViewById(R.id.server_connect)).setText("Connect to Server");
        }
        if(st.currentPage < 0) {
            changeToMain(false);
        } else {
            changeToSensor(st.currentPage, false);
        }
        st.setClientAddrsContainer(((TextView) findViewById(R.id.client_addrs)));

        //Listeners
        navigationView.setNavigationItemSelectedListener(this);
        findViewById(R.id.sensor_listen).setOnClickListener( this );
        findViewById(R.id.log_record).setOnClickListener( this );
        findViewById(R.id.client_start).setOnClickListener( this );
        findViewById(R.id.server_connect).setOnClickListener( this );

        ((EditText)findViewById(R.id.client_port)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    st.clientPort = Integer.parseInt(s.toString());
                } catch (Exception e) {}
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        ((EditText)findViewById(R.id.server_addr)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                st.serverAddr = s.toString();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        ((EditText)findViewById(R.id.server_port)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    st.serverPort = Integer.parseInt(s.toString());
                } catch (Exception e) {}
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        ((EditText)findViewById(R.id.server_username)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                st.serverUsername = s.toString();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        ((EditText)findViewById(R.id.server_password)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                st.serverPassword = s.toString();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    public void handle(Message msg) {
        switch (msg.what) {
            case AppMessages.MSG_SENSORS: doSensors(msg.getData().getStringArrayList("name"),
                    msg.getData().getIntegerArrayList("type"),
                    msg.getData().getIntegerArrayList("status")); break;
            case AppMessages.MSG_DATA: doData(msg.arg1, msg.getData().getFloatArray("values")); break;
        }
    }

    private void doData(int sensorId, float[] values) {
       // System.out.println("Get data from " + sensorId + " | Current " + st.currentPage);
        if(st.currentPage == sensorId) {
            if(values.length >= 1 ) {
                ((TextView) findViewById(R.id.ValueVal0)).setText(""+values[0]);
            }
            if(values.length >= 2 ) {
                ((TextView) findViewById(R.id.ValueVal1)).setText(""+values[1]);
            }
            if(values.length >= 3 ) {
                ((TextView) findViewById(R.id.ValueVal2)).setText(""+values[2]);
            }
            if(values.length >= 4 ) {
                ((TextView) findViewById(R.id.ValueVal3)).setText(""+values[3]);
            }
        }
    }

    private void doSensors(ArrayList<String> names, ArrayList<Integer> types, ArrayList<Integer> status) {
        System.out.println("Get sensors list");
        st.sensor_names = names;
        st.sensor_types = types;
        st.sensor_status = status;


        if(navigationView == null) {
            navigationView = (NavigationView) findViewById(R.id.nav_view);
        }
        SubMenu latMenu = navigationView.getMenu().getItem(0).getSubMenu();

        latMenu.clear();
        for(int i = 0; i< names.size(); i++){
            if(status.get(i) == AppMessages.LISTEN_METHOD_SHARE) {
                latMenu.add(0,i, 0, names.get(i)).setIcon(R.drawable.play);
            } else {
                latMenu.add(0,i, 0, names.get(i)).setIcon(R.drawable.pause);
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id < st.sensor_names.size()) {
            if(st.currentPage != id) {
                changeToSensor(id, true);
            }
        } else {
            if(st.currentPage != -1) {
                changeToMain(false);
            }
        }
        return false;
    }

    private void changeToMain(boolean checkSensor) {
        System.out.println("Change to main page");

        if(checkSensor) {
            int status = st.sensor_status.get(st.currentPage);
            if (status == AppMessages.LISTEN_METHOD_LISTEN) {
                ipc.send(AppMessages.listen(st.currentPage, AppMessages.LISTEN_METHOD_NO));
            }
        }
        st.currentPage = -1;

        findViewById(R.id.sensor_content).setVisibility(View.INVISIBLE);
        findViewById(R.id.main_content).setVisibility(View.VISIBLE);

    }

    private void changeToSensor(int id,boolean checkSensor) {
        System.out.println("Change to sensor "+id);

        if(checkSensor && st.currentPage >= 0) {
            int status = st.sensor_status.get(st.currentPage);
            if (status == AppMessages.LISTEN_METHOD_LISTEN) {
                ipc.send(AppMessages.listen(st.currentPage, AppMessages.LISTEN_METHOD_NO));
            }
        }


        st.currentPage = id;

        findViewById(R.id.main_content).setVisibility(View.INVISIBLE);
        findViewById(R.id.sensor_content).setVisibility(View.VISIBLE);

        String name = st.sensor_names.get(id);
        int type = st.sensor_types.get(id);
        int status = st.sensor_status.get(id);

        switch (status) {
            case AppMessages.LISTEN_METHOD_NO:
                ipc.send(AppMessages.listen(id, AppMessages.LISTEN_METHOD_LISTEN));
            case AppMessages.LISTEN_METHOD_LISTEN:
                ((Button) findViewById(R.id.sensor_listen)).setText("Share");
                break;
            case AppMessages.LISTEN_METHOD_SHARE:
                ((Button) findViewById(R.id.sensor_listen)).setText("Don't share");
                break;
        }

        ((TextView) findViewById(R.id.sensor_name)).setText(name);
        ((TextView) findViewById(R.id.sensor_type)).setText(getTypeString(type));

        ((TextView) findViewById(R.id.ValueVal0)).setText("-");
        ((TextView) findViewById(R.id.ValueVal1)).setText("-");
        ((TextView) findViewById(R.id.ValueVal2)).setText("-");
        ((TextView) findViewById(R.id.ValueVal3)).setText("-");
    }

    private String getTypeString(int type) {

        switch (type) {
            case 0 :
                return "GPS";
            case Sensor.TYPE_ACCELEROMETER :
                return "Accelerometer";
            case Sensor.TYPE_AMBIENT_TEMPERATURE :
                return "Ambient temperature";
            case Sensor.TYPE_GRAVITY :
                return "Gravity";
            case Sensor.TYPE_GYROSCOPE :
                return "Gyroscope";
            case Sensor.TYPE_LIGHT :
                return "Light";
            case Sensor.TYPE_LINEAR_ACCELERATION :
                return "Linear acceleration";
            case Sensor.TYPE_MAGNETIC_FIELD :
                return "Magnetic field";
            case Sensor.TYPE_ORIENTATION :
                return "Orientation";
            case Sensor.TYPE_PRESSURE :
                return "Pressure";
            case Sensor.TYPE_PROXIMITY :
                return "Proximity";
            case Sensor.TYPE_RELATIVE_HUMIDITY :
                return "Relative humidity";
            case Sensor.TYPE_ROTATION_VECTOR :
                return "Rotation vector";
            case Sensor.TYPE_TEMPERATURE :
                return "Temperature";
            default :
                return "Unknown sensor type";
        }
    }

    @Override
    public void onClick(View v) {
        if(findViewById(R.id.sensor_listen) == v) {
            doShare();
        } else if(findViewById(R.id.log_record) == v) {
            if(st.log) {
                System.out.println("UnRecord log");
                ipc.send(AppMessages.recordLog(false));
                ((Button) findViewById(R.id.log_record)).setText("Record Log");
                st.log = false;
            } else {
                System.out.println("Record log");
                ipc.send(AppMessages.recordLog(true));
                ((Button) findViewById(R.id.log_record)).setText("Stop recording Log");
                st.log = true;
            }
        } else if(findViewById(R.id.client_start) == v) {
            if(st.clientSt) {
                System.out.println("Stop client");
                ipc.send(AppMessages.client(st.clientPort, false));
                ((Button) findViewById(R.id.client_start)).setText("Start Client");
                st.clientSt = false;
            } else {
                System.out.println("Start client");
                ipc.send(AppMessages.client(st.clientPort, true));
                ((Button) findViewById(R.id.client_start)).setText("Stop Client");
                st.clientSt = true;
            }
        } else if(findViewById(R.id.server_connect) == v) {
            if(st.serverSt) {
                System.out.println("Stop server");
                ipc.send(AppMessages.server(st.serverAddr, st.serverPort, st.serverUsername, st.serverPassword, false));
                ((Button) findViewById(R.id.server_connect)).setText("Connect to Server");
                st.serverSt = false;
            } else {
                System.out.println("Start server");
                ipc.send(AppMessages.server(st.serverAddr, st.serverPort, st.serverUsername, st.serverPassword, true));
                ((Button) findViewById(R.id.server_connect)).setText("Disconnect from Server");
                st.serverSt = true;
            }
        }
    }

    private void doShare() {
        if (st.currentPage < 0) { return; }

        if(st.sensor_status.get(st.currentPage) != AppMessages.LISTEN_METHOD_SHARE) {
            ((Button)findViewById(R.id.sensor_listen)).setText("Don't Share");
            ipc.send(AppMessages.listen(st.currentPage, AppMessages.LISTEN_METHOD_SHARE));
        } else {
            ((Button)findViewById(R.id.sensor_listen)).setText("Share");
            ipc.send(AppMessages.listen(st.currentPage, AppMessages.LISTEN_METHOD_LISTEN));
        }
    }
}
