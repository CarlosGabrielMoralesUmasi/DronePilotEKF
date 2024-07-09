package com.dji.sdk.sample.demo.flightcontroller;
import android.os.Handler;
import android.app.Service;
import android.content.Context;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.internal.OnScreenJoystickListener;
import com.dji.sdk.sample.internal.controller.DJISampleApplication;
import com.dji.sdk.sample.internal.utils.DialogUtils;
import com.dji.sdk.sample.internal.utils.ModuleVerificationUtil;
import com.dji.sdk.sample.internal.utils.OnScreenJoystick;
import com.dji.sdk.sample.internal.utils.ToastUtils;
import com.dji.sdk.sample.internal.view.PresentableView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Timer;
import java.util.TimerTask;

import dji.common.error.DJIError;
import dji.common.flightcontroller.simulator.InitializationData;
import dji.common.flightcontroller.simulator.SimulatorState;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.model.LocationCoordinate2D;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.flightcontroller.Simulator;

/**
 * Class for virtual stick.
 */
public class VirtualStickView extends RelativeLayout implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, PresentableView {
    private Button btnEnableVirtualStick;
    private Button btnDisableVirtualStick;
    private Button btnHorizontalCoordinate;
    private Button btnSetYawControlMode;
    private Button btnSetVerticalControlMode;
    private Button btnSetRollPitchControlMode;
    private ToggleButton btnSimulator;
    private Button btnTakeOff;
    private Button btnLand;

    private Timer timer;

    private boolean girando1 = false;
    private boolean girando2 = false;

    private boolean isAscending = false;

    private TextView textView;

    private OnScreenJoystick screenJoystickRight;
    private OnScreenJoystick screenJoystickLeft;

    private Timer sendVirtualStickDataTimer;
    private SendVirtualStickDataTask sendVirtualStickDataTask;

    private float pitch;
    private float roll;
    private float yaw;
    private float throttle;
    private boolean isSimulatorActived = false;
    private FlightController flightController = null;
    private Simulator simulator = null;

    private DatabaseReference firebaseRef;

    private boolean cuadradoCompletado = false;

    Handler handler = new Handler();
    int tiempoTranscurrido = 0;
    int tiempoTranscurrido2 = 0;


    public VirtualStickView(Context context) {
        super(context);
        init(context);
    }

    @NonNull
    @Override
    public String getHint() {
        return this.getClass().getSimpleName() + ".java";
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setUpListeners();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (null != sendVirtualStickDataTimer) {
            if (sendVirtualStickDataTask != null) {
                sendVirtualStickDataTask.cancel();

            }
            sendVirtualStickDataTimer.cancel();
            sendVirtualStickDataTimer.purge();
            sendVirtualStickDataTimer = null;
            sendVirtualStickDataTask = null;
        }
        tearDownListeners();
        super.onDetachedFromWindow();
    }

    private void init(Context context) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.view_virtual_stick, this, true);
        initParams();
        initUI();

        //Init Firebase
        FirebaseApp.initializeApp(getContext());
        firebaseRef = FirebaseDatabase.getInstance().getReference("0001");

        ControlcuadradoFirebase();

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                FlightControllerFirebase();
            }
        }, 0, 100);




    }

    private void ControlcuadradoFirebase(){
        flightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                flightController.setVirtualStickAdvancedModeEnabled(true);
            }
        });
        firebaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Verifica si los datos existen
                if (dataSnapshot.exists()) {
                    Long cuadradoValue = dataSnapshot.child("cuadrado").getValue(Long.class);
                    Controlcuadrado(cuadradoValue);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void FlightControllerFirebase(){
        flightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                flightController.setVirtualStickAdvancedModeEnabled(true);
            }
        });
        firebaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Verifica si los datos existen
                if (dataSnapshot.exists()) {
                    Long xValue = dataSnapshot.child("x").getValue(Long.class);
                    Long yValue = dataSnapshot.child("y").getValue(Long.class);
                    Long zValue = dataSnapshot.child("z").getValue(Long.class);
                    Long Girovalue = dataSnapshot.child("giro").getValue(Long.class);

                    /////////////////////////////////////////////////////////   MOVIMIENTOS Z - SUBIR Y BAJAR    /////////////////////////////////////////////////////////////////////////
                    Controlz(zValue);
                    /////////////////////////////////////////////////////////   MOVIMIENTOS X - ADELANTE Y ATRAS    /////////////////////////////////////////////////////////////////////////
                    Controlx(xValue);
                    /////////////////////////////////////////////////////////   MOVIMIENTOS X - ADELANTE Y ATRAS    /////////////////////////////////////////////////////////////////////////
                    Controly(yValue);
                    /////////////////////////////////////////////////////////   MOVIMIENTO en Giro -     /////////////////////////////////////////////////////////////////////////
                    ControlGiro(Girovalue);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    private void Controlz(Long zValue){
        if (zValue.toString().equals("1")) {
            if (!isAscending) {
                flightController.startTakeoff(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        isAscending = true;
                    }
                });
                flightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        flightController.setVirtualStickAdvancedModeEnabled(true);
                    }
                });
            }
            else {
                float altura = flightController.getState().getAircraftLocation().getAltitude();
                // Configura la velocidad de ascenso (ajusta este valor según tus necesidades)
                if(altura < 1.0){
                    float ascentSpeed = 0.05f; // 1 metro por segundo, ajusta según lo necesario
                    flightController.sendVirtualStickFlightControlData(
                            new FlightControlData(0f, 0, 0, ascentSpeed), // Aumenta la velocidad vertical
                            new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if (djiError != null) {
                                        // Maneja errores
                                    }
                                }
                            });
                }
            }
        } else if (zValue.toString().equals("-1")){
            // Llama a la función de aterrizaje
            float altura = flightController.getState().getAircraftLocation().getAltitude();
            if(altura < 2.0){
                isAscending = false;
                flightController.startLanding(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        // Detén el ascenso en caso de aterrizaje
                    }
                });
            }else{
                // Configura la velocidad de ascenso (ajusta este valor según tus necesidades)
                float ascentSpeed = -0.05f; // 1 metro por segundo, ajusta según lo necesario
                flightController.sendVirtualStickFlightControlData(
                        new FlightControlData(0f, 0, 0, ascentSpeed), // Aumenta la velocidad vertical
                        new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if (djiError != null) {
                                    // Maneja errores
                                }
                            }
                        });
            }


        }
    }


    private void Controlx(Long xValue){
        if (xValue > 0) {
            float Speed = 0.05f* xValue;
            flightController.sendVirtualStickFlightControlData(
                    new FlightControlData(Speed, 0, 0, 0), // Aumenta la velocidad Horizontal
                    new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                // Maneja errores
                            }
                        }
                    });
        } else if (xValue < 0){
            float Speed = 0.05f * xValue;
            flightController.sendVirtualStickFlightControlData(
                    new FlightControlData(Speed, 0, 0, 0), // Aumenta la velocidad Horizontal
                    new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                // Maneja errores
                            }
                        }
                    });
        }
    }



    private void Controly(Long yValue){
        if (yValue > 0) {
            float Speed = 0.05f * yValue;
            flightController.sendVirtualStickFlightControlData(
                    new FlightControlData(0, Speed, 0, 0), // Aumenta la velocidad Horizontal
                    new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                // Maneja errores
                            }
                        }
                    });
        } else if (yValue < 0){
            float Speed = 0.05f * yValue;
            flightController.sendVirtualStickFlightControlData(
                    new FlightControlData(0, Speed, 0, 0), // Aumenta la velocidad Horizontal
                    new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                // Maneja errores
                            }
                        }
                    });
        }
    }


    private void ControlGiro(Long GiroValue){
        if (GiroValue.toString().equals("1")) {
            if(girando2 == true){
                flightController.sendVirtualStickFlightControlData(
                        new FlightControlData(0, 0, 0, 0),
                        new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if (djiError != null) {
                                    // Maneja errores
                                }
                            }
                        });
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                girando2 = false;
            }
            float Speed = GiroValue * 10f;
            flightController.sendVirtualStickFlightControlData(
                    new FlightControlData(0, 0, Speed, 0),
                    new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            girando1 = true;
                            if (djiError != null) {
                                // Maneja errores
                            }
                        }
                    });
        }

        else if (GiroValue.toString().equals("-1")){
            if(girando1 == true){
                flightController.sendVirtualStickFlightControlData(
                        new FlightControlData(0, 0, 0, 0),
                        new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if (djiError != null) {
                                    // Maneja errores
                                }
                            }
                        });
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                girando1 = false;
            }
            float Speed = GiroValue * 10f;
            flightController.sendVirtualStickFlightControlData(
                    new FlightControlData(0, 0, Speed, 0),
                    new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            girando2 = true;
                            if (djiError != null) {
                                // Maneja errores
                            }
                        }
                    });
        }
    }

    private void Controlcuadrado(Long cuadradoValue) {
        if (!cuadradoCompletado && cuadradoValue > 0) {
            // Mientras el cuadrado no esté completado y cuadradoValue sea positivo, realizar movimientos para formar el cuadrado
            // Primer movimiento hacia adelante durante 7 segundos
            long startTime1 = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime1 < 10000) {
                // Mover hacia adelante
                Controly(-1L);
                try {
                    Thread.sleep(1); // Pequeña pausa para no saturar el procesador
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }


            try {
                Thread.sleep(10); // Pequeña pausa para no saturar el procesador
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Segundo movimiento (giro) durante 3 segundos
            long startTime2 = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime2 < 9000) {
                // Realizar giro
                ControlGiro(1L);
                try {
                    Thread.sleep(1); // Pequeña pausa para no saturar el procesador
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            try {
                Thread.sleep(10); // Pequeña pausa para no saturar el procesador
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            startTime1 = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime1 < 10000) {
                // Mover hacia adelante
                Controly(-1L);
                try {
                    Thread.sleep(1); // Pequeña pausa para no saturar el procesador
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            try {
                Thread.sleep(10); // Pequeña pausa para no saturar el procesador
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Segundo movimiento (giro) durante 3 segundos
            startTime2 = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime2 < 9000) {
                // Realizar giro
                ControlGiro(1L);
                try {
                    Thread.sleep(1); // Pequeña pausa para no saturar el procesador
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            try {
                Thread.sleep(10); // Pequeña pausa para no saturar el procesador
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            startTime1 = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime1 < 10000) {
                // Mover hacia adelante
                Controly(-1L);
                try {
                    Thread.sleep(1); // Pequeña pausa para no saturar el procesador
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }


            try {
                Thread.sleep(10); // Pequeña pausa para no saturar el procesador
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Segundo movimiento (giro) durante 3 segundos
            startTime2 = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime2 < 9000) {
                // Realizar giro
                ControlGiro(1L);
                try {
                    Thread.sleep(1); // Pequeña pausa para no saturar el procesador
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            try {
                Thread.sleep(10); // Pequeña pausa para no saturar el procesador
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            startTime1 = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime1 < 10000) {
                // Mover hacia adelante
                Controly(-1L);
                try {
                    Thread.sleep(1); // Pequeña pausa para no saturar el procesador
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            try {
                Thread.sleep(10); // Pequeña pausa para no saturar el procesador
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            // Se establece que el cuadrado está completado
            cuadradoCompletado = true;

            // Mensaje de fin
            ToastUtils.setResultToToast("Fin");
            firebaseRef.child("cuadrado").setValue(0L);

        }
    }




    //////////////


    private void initParams() {
        // We recommand you use the below settings, a standard american hand style.
        if (flightController == null) {
            if (ModuleVerificationUtil.isFlightControllerAvailable()) {
                flightController = DJISampleApplication.getAircraftInstance().getFlightController();
            }
        }
        flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
        flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
        flightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
        flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);

        // Check if the simulator is activated.
        if (simulator == null) {
            simulator = ModuleVerificationUtil.getSimulator();
        }
        isSimulatorActived = simulator.isSimulatorActive();

    }

    private void initUI() {
        btnEnableVirtualStick = (Button) findViewById(R.id.btn_enable_virtual_stick);
        btnDisableVirtualStick = (Button) findViewById(R.id.btn_disable_virtual_stick);
        btnHorizontalCoordinate = (Button) findViewById(R.id.btn_horizontal_coordinate);
        btnSetYawControlMode = (Button) findViewById(R.id.btn_yaw_control_mode);
        btnSetVerticalControlMode = (Button) findViewById(R.id.btn_vertical_control_mode);
        btnSetRollPitchControlMode = (Button) findViewById(R.id.btn_roll_pitch_control_mode);
        btnTakeOff = (Button) findViewById(R.id.btn_take_off);

        btnLand = (Button) findViewById(R.id.btn_land);
        btnLand.setOnClickListener(this);

        btnSimulator = (ToggleButton) findViewById(R.id.btn_start_simulator);

        textView = (TextView) findViewById(R.id.textview_simulator);

        screenJoystickRight = (OnScreenJoystick) findViewById(R.id.directionJoystickRight);
        screenJoystickLeft = (OnScreenJoystick) findViewById(R.id.directionJoystickLeft);

        btnEnableVirtualStick.setOnClickListener(this);
        btnDisableVirtualStick.setOnClickListener(this);
        btnHorizontalCoordinate.setOnClickListener(this);
        btnSetYawControlMode.setOnClickListener(this);
        btnSetVerticalControlMode.setOnClickListener(this);
        btnSetRollPitchControlMode.setOnClickListener(this);
        btnTakeOff.setOnClickListener(this);
        btnSimulator.setOnCheckedChangeListener(VirtualStickView.this);

        if (isSimulatorActived) {
            btnSimulator.setChecked(true);
            textView.setText("Simulator is On.");
        }
    }

    private void setUpListeners() {
        if (simulator != null) {
            simulator.setStateCallback(new SimulatorState.Callback() {
                @Override
                public void onUpdate(@NonNull final SimulatorState simulatorState) {
                    ToastUtils.setResultToText(textView,
                            "Yaw : "
                                    + simulatorState.getYaw()
                                    + ","
                                    + "X : "
                                    + simulatorState.getPositionX()
                                    + "\n"
                                    + "Y : "
                                    + simulatorState.getPositionY()
                                    + ","
                                    + "Z : "
                                    + simulatorState.getPositionZ());
                }
            });
        } else {
            ToastUtils.setResultToToast("Simulator disconnected!");
        }

        screenJoystickLeft.setJoystickListener(new OnScreenJoystickListener() {

            @Override
            public void onTouch(OnScreenJoystick joystick, float pX, float pY) {
                if (Math.abs(pX) < 0.02) {
                    pX = 0;
                }

                if (Math.abs(pY) < 0.02) {
                    pY = 0;
                }
                float pitchJoyControlMaxSpeed = 10;
                float rollJoyControlMaxSpeed = 10;

                pitch = pitchJoyControlMaxSpeed * pY;
                roll = rollJoyControlMaxSpeed * pX;

                if (null == sendVirtualStickDataTimer) {
                    sendVirtualStickDataTask = new SendVirtualStickDataTask();
                    sendVirtualStickDataTimer = new Timer();
                    sendVirtualStickDataTimer.schedule(sendVirtualStickDataTask, 100, 200);
                }
            }
        });

        screenJoystickRight.setJoystickListener(new OnScreenJoystickListener() {

            @Override
            public void onTouch(OnScreenJoystick joystick, float pX, float pY) {
                if (Math.abs(pX) < 0.02) {
                    pX = 0;
                }

                if (Math.abs(pY) < 0.02) {
                    pY = 0;
                }
                float verticalJoyControlMaxSpeed = 4;
                float yawJoyControlMaxSpeed = 20;

                yaw = yawJoyControlMaxSpeed * pX;
                throttle = verticalJoyControlMaxSpeed * pY;

                if (null == sendVirtualStickDataTimer) {
                    sendVirtualStickDataTask = new SendVirtualStickDataTask();
                    sendVirtualStickDataTimer = new Timer();
                    sendVirtualStickDataTimer.schedule(sendVirtualStickDataTask, 0, 200);
                }
            }
        });
    }

    private void tearDownListeners() {
        Simulator simulator = ModuleVerificationUtil.getSimulator();
        if (simulator != null) {
            simulator.setStateCallback(null);
        }
        screenJoystickLeft.setJoystickListener(null);
        screenJoystickRight.setJoystickListener(null);
    }

    @Override
    public void onClick(View v) {
        FlightController flightController = ModuleVerificationUtil.getFlightController();
        if (flightController == null) {
            return;
        }
        switch (v.getId()) {
            case R.id.btn_enable_virtual_stick:
                flightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        flightController.setVirtualStickAdvancedModeEnabled(true);
                        DialogUtils.showDialogBasedOnError(getContext(), djiError);
                    }
                });
                break;

            case R.id.btn_disable_virtual_stick:
                flightController.setVirtualStickModeEnabled(false, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        DialogUtils.showDialogBasedOnError(getContext(), djiError);
                    }
                });
                break;

            case R.id.btn_land:
                flightController.startLanding(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError == null) {
                            // Aterrizaje iniciado con éxito
                            Toast.makeText(getContext(), "Aterrizaje iniciado", Toast.LENGTH_SHORT).show();
                        } else {
                            // Maneja el error
                            Toast.makeText(getContext(), "Error al iniciar el aterrizaje: " + djiError.getDescription(), Toast.LENGTH_SHORT).show();
                            DialogUtils.showDialogBasedOnError(getContext(), djiError);
                        }
                    }
                });
                break;


                /*
                case R.id.btn_land:
                    // Verifica si el valor de z es igual a 0
                    DatabaseReference zRef = firebaseRef.child("z");
                    zRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            Long zValue = dataSnapshot.getValue(Long.class);
                            if (zValue != null && zValue == 0) {
                                // Aterriza el dron
                                flightController.startLanding(new CommonCallbacks.CompletionCallback() {
                                    @Override
                                    public void onResult(DJIError djiError) {
                                        if (djiError == null) {
                                            Toast.makeText(getContext(), "Aterrizaje iniciado", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(getContext(), "Error al iniciar el aterrizaje: " + djiError.getDescription(), Toast.LENGTH_SHORT).show();
                                            DialogUtils.showDialogBasedOnError(getContext(), djiError);
                                        }
                                    }
                                });
                            } else {
                                // Si z no es 0, muestra un mensaje
                                Toast.makeText(getContext(), "El valor de z no es 0", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.e("FirebaseData", "Error al obtener datos de Firebase: " + databaseError.getMessage());
                        }
                    });
                    break;
                * */
            case R.id.btn_roll_pitch_control_mode:
                if (flightController.getRollPitchControlMode() == RollPitchControlMode.VELOCITY) {
                    flightController.setRollPitchControlMode(RollPitchControlMode.ANGLE);
                } else {
                    flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
                }
                ToastUtils.setResultToToast(flightController.getRollPitchControlMode().name());
                break;
            case R.id.btn_yaw_control_mode:
                if (flightController.getYawControlMode() == YawControlMode.ANGULAR_VELOCITY) {
                    flightController.setYawControlMode(YawControlMode.ANGLE);
                } else {
                    flightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
                }
                ToastUtils.setResultToToast(flightController.getYawControlMode().name());
                break;
            case R.id.btn_vertical_control_mode:
                if (flightController.getVerticalControlMode() == VerticalControlMode.VELOCITY) {
                    flightController.setVerticalControlMode(VerticalControlMode.POSITION);
                } else {
                    flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
                }
                ToastUtils.setResultToToast(flightController.getVerticalControlMode().name());
                break;
            case R.id.btn_horizontal_coordinate:
                if (flightController.getRollPitchCoordinateSystem() == FlightCoordinateSystem.BODY) {
                    flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.GROUND);
                } else {
                    flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
                }
                ToastUtils.setResultToToast(flightController.getRollPitchCoordinateSystem().name());
                break;
            case R.id.btn_take_off:
                flightController.startTakeoff(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        DialogUtils.showDialogBasedOnError(getContext(), djiError);
                    }
                });
                break;
            default:
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (compoundButton == btnSimulator) {
            onClickSimulator(b);
        }
    }

    private void onClickSimulator(boolean isChecked) {
        if (simulator == null) {
            return;
        }
        if (isChecked) {
            textView.setVisibility(VISIBLE);
            simulator.start(InitializationData.createInstance(new LocationCoordinate2D(23, 113), 10, 10), new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        ToastUtils.setResultToToast(djiError.getDescription());
                    }
                }
            });
        } else {
            textView.setVisibility(INVISIBLE);
            simulator.stop(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        ToastUtils.setResultToToast(djiError.getDescription());
                    }
                }
            });
        }
    }

    @Override
    public int getDescription() {
        return R.string.flight_controller_listview_virtual_stick;
    }

    private class SendVirtualStickDataTask extends TimerTask {
        @Override
        public void run() {
            if (flightController != null) {
                //接口写反了，setPitch()应该传入roll值，setRoll()应该传入pitch值
                flightController.sendVirtualStickFlightControlData(new FlightControlData(roll, pitch, yaw, throttle), new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            ToastUtils.setResultToToast(djiError.getDescription());
                        }
                    }
                });
            }
        }
    }

}
