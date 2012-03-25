package com.tejus.shavedoggery.core;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import org.json.JSONException;
import org.json.JSONObject;

import com.tejus.shavedoggery.util.Logger;

import android.content.Context;
import android.os.AsyncTask;

public class HeartBeatServer extends AsyncTask<Void, Object, Object> {
    Socket mSocket;
    long pingId;
    JSONObject mData = new JSONObject();
    Context mContext;
    byte[] payload;
    OutputStream oStream;
    public static boolean sendHeartBeats = true;

    public HeartBeatServer( Context context, Socket socket ) {
        mContext = context;
        mSocket = socket;

        try {
            mData.put( "username", "notashavedog" );
            mData.put( Definitions.PACKET_TYPE, Definitions.TYPE_HEARTBEAT );
            payload = mData.toString().getBytes();
            oStream = ( OutputStream ) mSocket.getOutputStream();
        } catch ( JSONException e ) {
            e.printStackTrace();
        } catch ( IOException e ) {
            Logger.error( "HeartBeatServer.HeartBeatServer(): Error creating an oStream on our Socket " );
            e.printStackTrace();
        }

    }

    @Override
    protected Object doInBackground( Void... args ) {
        try {
            while ( sendHeartBeats ) {
                Thread.sleep( Definitions.HEARTBEAT_INTERVAL );
                Logger.trace( "HeartBeatServer.doInBackground(): gonna send HeartBeat ping.." );
                oStream.write( payload );
            }
        } catch ( IOException e ) {
            Logger.error( "HeartBeatServer.doInBackground(): Error writing to our oStream " );
            e.printStackTrace();
        } catch ( InterruptedException e ) {
            e.printStackTrace();
        }
        return null;
    }

}
