package com.proyecto.discretas.cerradura2;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.UUID;

public class Comunicacion extends AppCompatActivity {

    //variables de control de la vista
    //--------------------
    Button enviar;
    Button abrir;
    Button cerrar;
    EditText pwd;
    TextView msjConfimacion;
    //---------------------

    //Variables para la conexión bluetooth
    //-------------------------------------------
    Handler bluetoothIn;
    final int handlerState = 0;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder DataStringIN = new StringBuilder();
    private ConnectedThread MyConexionBT;
    // Identificador unico de servicio - SPP UUID
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // String para la direccion MAC
    private static String address = null;
    //-------------------------------------------


    //PARAMETROS PARA LA ENCRIPTACIÓN
    //-------------------------------------------
    //Numero Primo que define el Grupo
    private long p=134217779;
    //Parametros de la curva y^2 = x^3 +ax+b
    private long a=13;
    private long b=17;
    //Factor Espaciamiento H
    private long h;
    //Creación de la curva
    Curva C=new Curva(p,a,b,224);
    //clave publica del arduino
    private Punto publicKeyB=new Punto(104513403,24487958);
    //Definiendo el punto generador
    Punto G=new Punto(566031,86796771);
    Sistema sistema=new Sistema(C,G);
    //-----------------------------------------------


    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comunicacion);
        this.h=C.getH();

        //instancia de los botones
        enviar=(Button) findViewById(R.id.envioPwd);
        abrir=(Button) findViewById(R.id.abrir);
        cerrar=(Button) findViewById(R.id.cerrar);
        //----------------------------------

        //Instancia del EditText de la contraseña
        pwd=(EditText) findViewById(R.id.pwd);
        //-----------------------------------

        //instancia  del mensaje de confirmacion (TextView)
        msjConfimacion=(TextView) findViewById(R.id.txtConfirmacion);
        //---------------------------------------
        msjConfimacion.setText(R.string.txtNegacion);




        //Manejador del mensaje que recibe la app de la comunicación
        //-------------------------------------------------------
        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {

                    String readMessage = (String) msg.obj;
                    StringTokenizer msjToken;
                    DataStringIN.append(readMessage);

                    //se lee el mensaje hasta que encuentre el caracter "#"
                    int endOfLineIndex = DataStringIN.indexOf("#");

                    if (endOfLineIndex > 0) {
                        //mensaje que guardará los caracteres desencriptados
                        StringBuilder msjFinal=new StringBuilder();
                        //--Punto M
                        Punto Mm,Nn;
                        //"dataInPrint" guarda una subcadena del mensaje en donde...
                        //...no se contempla el ultimo caraceter.
                        String dataInPrint = DataStringIN.substring(0, endOfLineIndex);
                        //Se separa en tokens la subcadena
                        msjToken=new StringTokenizer(dataInPrint,"|");
                        //Se guarda el primer valor valor recibido como M
                        String m=msjToken.nextToken();
                        Mm=Conversion.stringToPunto(m);


                        //----Se obtiene el valor de N
                        //------------------------------------
                        while(msjToken.hasMoreTokens()) {

                            String n = msjToken.nextToken();
                            Nn=Conversion.stringToPunto(n);
                            msjFinal.append(sistema.desEncriptar(Mm,Nn));

                        }//------------------------------------

                        //Si se confirma la contraseña se manda un mensaje d confirmacion
                        //y se configura lo que harán los botones de "abrir" y "cerrar".
                        //----------------------------------------------------------
                        if(msjFinal.toString().equals("S")){
                            msjConfimacion.setText(R.string.txtConfirmacion);

                            abrir.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    msjConfimacion.setText(R.string.msjAbierto);
                                    MyConexionBT.write("1");
                                }
                            });

                            cerrar.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    msjConfimacion.setText(R.string.msjCerrado);
                                    MyConexionBT.write("0");
                                }
                            });
                        }
                        //----------------------------------------------------------
                        if(msjFinal.toString().equals("N")){
                            Toast.makeText(getBaseContext(),"La contraseña es invalida",Toast.LENGTH_SHORT).show();
                            msjConfimacion.setText(R.string.txtNegacion);
                        }


                        DataStringIN.delete(0, DataStringIN.length());
                    }
                }
            }
        };
        //----------------------------------------------------------
        btAdapter = BluetoothAdapter.getDefaultAdapter(); // get Bluetooth adapter
        VerificarEstadoBT();




        //Accion del boton "Enviar"
        enviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Se codifica cada caracter de la contraseña y se encripta
                //---------------------------------------------------------------

                //Se crea el arreglo de Puntos de N
                ArrayList<Punto> N=new ArrayList<>();

                String pass=pwd.getText().toString();
                for(int i=0;i<pass.length();i++){

                    Punto codificado=Mensaje.Codificar(pass.charAt(i),h,a,b,p);
                    //se guardan los puntos encriptados en el ArrayList N
                    N.add(sistema.encriptar(codificado,publicKeyB));

                }
                //-------------------------------------------------------------

                //se manda la contraseña encriptada con los valores (M,N)
                MyConexionBT.write("Pass:\n"+"M:\n"+sistema.getkG());
                MyConexionBT.write("N:\n");
                for(int i=0;i<N.size();i++)
                    MyConexionBT.write(N+";");



            }
        });



    }


    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException
    {
        //crea un conexion de salida segura para el dispositivo
        //usando el servicio UUID
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        //Consigue la direccion MAC desde DeviceListActivity via intent
        Intent intent = getIntent();
        //Consigue la direccion MAC desde DeviceListActivity via EXTRA
        address = intent.getStringExtra(ListBluetooth.EXTRA_DEVICE_ADDRESS);
        //Setea la direccion MAC
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        try
        {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "La creacción del Socket fallo", Toast.LENGTH_LONG).show();
        }
        // Establece la conexión con el socket Bluetooth.
        try
        {
            btSocket.connect();
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "Fallo en la conexion del socket del bluetooth", Toast.LENGTH_LONG).show();

            try {
                btSocket.close();
            } catch (IOException e2) {
                Toast.makeText(getBaseContext(), "Ocurrio un error al cerrar la comunicacion bluetooth", Toast.LENGTH_LONG).show();
            }
        }
        MyConexionBT = new ConnectedThread(btSocket);
        MyConexionBT.start();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        try
        { // Cuando se sale de la aplicación esta parte permite
            // que no se deje abierto el socket
            btSocket.close();
        } catch (IOException e2) {
            Toast.makeText(getBaseContext(), "Ocurrio un error al cerrar la comunicacion bluetooth", Toast.LENGTH_LONG).show();

        }
    }

    //Comprueba que el dispositivo Bluetooth Bluetooth está disponible y solicita que se active si está desactivado
    private void VerificarEstadoBT() {

        if(btAdapter==null) {
            Toast.makeText(getBaseContext(), "El dispositivo no soporta bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (btAdapter.isEnabled()) {
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    //Crea la clase que permite crear el evento de conexion
    private class ConnectedThread extends Thread
    {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket)
        {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try
            {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Toast.makeText(getBaseContext(), "Ocurrio un error con el flujo de datos", Toast.LENGTH_LONG).show();

            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run()
        {
            byte[] buffer = new byte[256];
            int bytes;

            // Se mantiene en modo escucha para determinar el ingreso de datos
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    // Envia los datos obtenidos hacia el evento via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    Toast.makeText(getBaseContext(), "Ocurrio un error en la escucha de datos", Toast.LENGTH_LONG).show();

                    break;
                }
            }
        }
        //Envio de trama
        public void write(String input)
        {
            try {
                mmOutStream.write(input.getBytes());
            }
            catch (IOException e)
            {
                //si no es posible enviar datos se cierra la conexión
                Toast.makeText(getBaseContext(), "La Conexión fallo", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

}
