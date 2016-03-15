package com.anki.basichomecontrol;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
  
public class MainActivity extends Activity {
  private static final String TAG = "bluetooth2";
    
  EditText eTBAddress , eTSend;
  
  Button bConnect,bOn, bOff ,bSend ;
  TextView tvResponse;
  Handler h;
    
  final int RECIEVE_MESSAGE = 1;        // Status  for Handler
  private BluetoothAdapter btAdapter = null;
  private BluetoothSocket btSocket = null;
  private StringBuilder sb = new StringBuilder();
   
  private ConnectedThread mConnectedThread;
    
  // SPP UUID service
  private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
  
  // MAC-address of Bluetooth module (you must edit this line)
  private static String address ;  //"00:25:56:DE:C4:85";
    
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  
    setContentView(R.layout.activity_main);
  initializeViews();
  btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
  checkBTState();

  
  bConnect.setOnClickListener(new OnClickListener(){

	@Override
	public void onClick(View arg0) {
		connect(); 	
	}
	  
  });
 
  bSend.setOnClickListener(new OnClickListener(){

	@Override
	public void onClick(View arg0) {
		
	mConnectedThread.write(eTSend.getText().toString());
		
	}
	  
  });
  
  
  
    h = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
            case RECIEVE_MESSAGE:                                                   // if receive massage
                byte[] readBuf = (byte[]) msg.obj;
                String strIncom = new String(readBuf, 0, msg.arg1);                 // create string from bytes array
                sb.append(strIncom);                                                // append string
                int endOfLineIndex = sb.indexOf("\r\n");                            // determine the end-of-line
                if (endOfLineIndex > 0) {                                            // if end-of-line,
                    String sbprint = sb.substring(0, endOfLineIndex);               // extract string
                    sb.delete(0, sb.length());                                      // and clear
                    tvResponse.setText("Data from avr: " + sbprint);            // update TextView
                    bOff.setEnabled(true);
                    bOn.setEnabled(true);
                }
                //Log.d(TAG, "...String:"+ sb.toString() +  "Byte:" + msg.arg1 + "...");
                break;
            }
        };
    };
      
    
    
   
    bOn.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        bOn.setEnabled(false);
        bOff.setEnabled(true); 
        mConnectedThread.write("1");    // Send "1" via Bluetooth
        //Toast.makeText(getBaseContext(), "Turn on LED", Toast.LENGTH_SHORT).show();
      }
    });
  
    bOff.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        bOff.setEnabled(false);   
        bOn.setEnabled(true);   
        mConnectedThread.write("0");    // Send "0" via Bluetooth
        //Toast.makeText(getBaseContext(), "Turn off LED", Toast.LENGTH_SHORT).show();
      }
    }); 
  }
   
  private void initializeViews() {

	 eTBAddress=(EditText) findViewById(R.id.eTBAddress); //et for bt address of the server
	 bConnect = (Button) findViewById(R.id.bConnect); //to connect to server
	 bOn = (Button) findViewById(R.id.bOn);
	 bOff = (Button) findViewById(R.id.bOff);
	 bSend = (Button) findViewById(R.id.bSend);
	 eTSend= (EditText) findViewById(R.id.eTSend);
	 tvResponse =(TextView) findViewById(R.id.tvResponse);
	
}

private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
      if(Build.VERSION.SDK_INT >= 10){
          try {
              final Method  m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
              return (BluetoothSocket) m.invoke(device, MY_UUID);
          } catch (Exception e) {
              Log.e(TAG, "Could not create Insecure RFComm Connection",e);
          }
      }
      return  device.createRfcommSocketToServiceRecord(MY_UUID);
  }
    



  public void connect() {
    address = eTBAddress.getText().toString();
    // Set up a pointer to the remote node using it's address.
    BluetoothDevice device = btAdapter.getRemoteDevice(address);
    
    // Two things are needed to make a connection:
    //   A MAC address, which we got above.
    //   A Service ID or UUID.  In this case we are using the
    //     UUID for SPP.
     
    try {
    	
        btSocket = createBluetoothSocket(device);
        
    } catch (IOException e) {
        errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
    }
    
    // Discovery is resource intensive.  Make sure it isn't going on
    // when you attempt to connect and pass your message.
    btAdapter.cancelDiscovery();
    
    // Establish the connection.  This will block until it connects.
    Log.d(TAG, "...Connecting...");
    try {
      btSocket.connect();
     
      Log.d(TAG, "....Connection ok...");
    } catch (IOException e) {
      try {
        btSocket.close();
      } catch (IOException e2) {
        errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
      }
    }
      
    // Create a data stream so we can talk to server.
    Log.d(TAG, "...Create Socket...");
    
    mConnectedThread = new ConnectedThread(btSocket);
    mConnectedThread.start();
  }
  

  
  @Override
protected void onDestroy() {
	// TODO Auto-generated method stub
	super.onDestroy();
	

    Log.d(TAG, "...In onDestroy()...");
   if(btSocket != null){
    try     {
      btSocket.close();
    } catch (IOException e2) {
      errorExit("Fatal Error", "In onDestroy() and failed to close socket." + e2.getMessage() + ".");
    }
   }
	
}

@Override
  public void onPause() {
    super.onPause();
  
    Log.d(TAG, "...In onPause()...");
    if(btSocket != null){
    try     {
      btSocket.close();
    } catch (IOException e2) {
      errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
    }
    }
  }
    
  
  
  
  
  
  
  
  private void checkBTState() {
    // Check for Bluetooth support and then check to make sure it is turned on
    // Emulator doesn't support Bluetooth and will return null
    if(btAdapter==null) {
      errorExit("Fatal Error", "Bluetooth not support");
    } else {
      if (btAdapter.isEnabled()) {
        Log.d(TAG, "...Bluetooth ON...");
      } else {
        //Prompt user to turn on Bluetooth
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, 1);
      }
    }
  }
  
  

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
  
  
  private void errorExit(String title, String message){
    Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
    finish();
  }
  
  private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
      PrintWriter pw;
        //this is the constructor
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
      
            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }
      
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
     
        }
      
        public void run() {
            byte[] buffer = new byte[256];  // buffer store for the stream
            int bytes; // bytes returned from read()
 
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);        // Get number of bytes and message in "buffer"
                    h.obtainMessage(RECIEVE_MESSAGE, bytes, -1, buffer).sendToTarget();     // Send to message queue Handler
                } catch (IOException e) {
                    break;
                }
            }
        }
      
        /* Call this from the main activity to send data to the remote device */
        public void write(String message) {
            Log.d(TAG, "...Data to send: " + message + "...");
            byte[] msgBuffer = message.getBytes();
            pw =new PrintWriter(mmOutStream,true);
            pw.println(new String(msgBuffer));
        }
    }
}
	