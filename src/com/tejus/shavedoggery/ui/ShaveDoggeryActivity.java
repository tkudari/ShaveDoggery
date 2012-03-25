package com.tejus.shavedoggery.ui;

import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.json.JSONException;
import org.json.JSONObject;


import com.tejus.shavedoggery.R;
import com.tejus.shavedoggery.core.Definitions;
import com.tejus.shavedoggery.core.ShaveService;
import com.tejus.shavedoggery.util.Logger;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceView;

public class ShaveDoggeryActivity extends Activity {

    Socket socket = null;
    BroadcastReceiver mShaveReceiver = new ServiceIntentReceiver();
    ServiceConnection mConnection;
    ShaveService mShaveService;
    CameraView mCameraView;
    int targetWidth = 320;
    int targetHeight = 240;
    final String videoFile = "/sdcard/ShaveVideo.mp4";


    /** Called when the activity is first created. */
    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.main );
        // TODO: change this, obv.
        Definitions.OUR_USERNAME = "ashavedog";
        initShaveServiceStuff();
        // initReceiver();

    }

    @Override
    public void onStart() {
        super.onStart();
        mCameraView = ( CameraView ) findViewById( R.id.surface_overlay );
        SurfaceView sv = ( SurfaceView ) findViewById( R.id.surface_camera );
        mCameraView.setupCamera( sv );
        mCameraView.prepareMedia(targetWidth, targetHeight);
        boolean ret = mCameraView.startRecording(videoFile);


    }

    private void initReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction( Definitions.INTENT_INCOMING_FILE_REQUEST );
        registerReceiver( mShaveReceiver, filter );
    }

    void initShaveServiceStuff() {
        mConnection = new ServiceConnection() {
            @Override
            public void onServiceDisconnected( ComponentName className ) {
                mShaveService = null;
            }

            @Override
            public void onServiceConnected( ComponentName name, IBinder service ) {
                mShaveService = ( ( ShaveService.ShaveBinder ) service ).getService();
            }
        };

        doBindService();
        startService( new Intent().setClass( this, ShaveService.class ) );
    }

    void doBindService() {
        bindService( new Intent( this, ShaveService.class ), mConnection, Context.BIND_AUTO_CREATE );
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate( R.menu.shave_menu, menu );
        return true;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        switch ( item.getItemId() ) {
            case R.id.test_api:
                this.testApi();
                return true;

            case R.id.test_api2:
                this.testApi2();
                return true;

            case R.id.quit:
                this.quit();
                return true;

            default:
                return super.onOptionsItemSelected( item );
        }
    }

    private void quit() {
        Logger.debug( "ShaveDoggery.quit(): Killing ourself.." );
        android.os.Process.killProcess( android.os.Process.myPid() );
    }

    private void testApi() {
        JSONObject data = new JSONObject();
        try {
            data.put( "packet_type", "file_push_req" );
            data.put( "file_size", 130130 );
            data.put( "file_name", Environment.getExternalStorageDirectory().toString() + "/Eagles of Death Metal - Heart On 02 Wannabe in LA.mp3" );
            data.put( "to", "ashavedog" );
            data.put( "username", Definitions.OUR_USERNAME );

            mShaveService.sendMessage( data );
        } catch ( JSONException e ) {
            e.printStackTrace();
        }

    }

    private byte[] reply = new byte[ 40 ];

    private void testApi2() {
        JSONObject data = new JSONObject();
        try {
            data.put( "username", Definitions.OUR_USERNAME );
            data.put( "packet_type", "bootup" );
            mShaveService.sendMessage( data );
        } catch ( JSONException e ) {
            e.printStackTrace();
        }
    }

    private InetAddress getOurIp() {
        WifiManager wifi = ( WifiManager ) this.getSystemService( Context.WIFI_SERVICE );
        DhcpInfo dhcp = wifi.getDhcpInfo();
        int ourIp = dhcp.ipAddress;
        byte[] quads = new byte[ 4 ];
        try {
            for ( int k = 0; k < 4; k++ ) {
                quads[ k ] = ( byte ) ( ( ourIp >> k * 8 ) & 0xFF );
            }

            return InetAddress.getByAddress( quads );
        } catch ( UnknownHostException e ) {
            e.printStackTrace();
        }
        return null;

    }

    private class ServiceIntentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive( Context context, Intent intent ) {
            String action = intent.getAction();
            if ( action.equals( "incoming_file_request" ) ) {

            }
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        // unregisterReceiver( mShaveReceiver );
    }
}