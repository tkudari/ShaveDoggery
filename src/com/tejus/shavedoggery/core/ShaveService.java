package com.tejus.shavedoggery.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import org.json.JSONException;
import org.json.JSONObject;

import com.tejus.shavedoggery.R;
import com.tejus.shavedoggery.util.Logger;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.widget.Toast;

public class ShaveService extends Service {

    private final IBinder mBinder = new ShaveBinder();
    private Socket mSocket;
    OutputStream oStream;
    InputStream iStream;
    Context activityContext;

    @Override
    public IBinder onBind( Intent arg0 ) {
        return mBinder;
    }

    public class ShaveBinder extends Binder {
        public ShaveService getService() {
            return ShaveService.this;
        }
    }

    @Override
    public void onCreate() {
        Logger.info( "service createdd" );
        setupConnections();
        // JSONObject data = new JSONObject();
        // try {
        // data.put( "username", Definitions.OUR_USERNAME );
        // data.put( "packet_type", "bootup" );
        // sendMessage( data );
        // } catch ( JSONException e ) {
        // e.printStackTrace();
        // }
    }

    @Override
    public int onStartCommand( Intent intent, int flags, int startId ) {

        return START_STICKY;
    }

    private void setupConnections() {
        try {
            mSocket = new Socket( Definitions.SERVER_IP, Definitions.SERVER_PORT );
            oStream = ( OutputStream ) mSocket.getOutputStream();
            iStream = ( InputStream ) mSocket.getInputStream();
        } catch ( UnknownHostException e ) {
            e.printStackTrace();
        } catch ( IOException e ) {
            e.printStackTrace();
        }

        new MessageListener().execute( iStream );
    }

    public void sendMessage( JSONObject data ) {
        try {
            Logger.info( "ShaveService.sendMessage: gonna send message = " + data.toString() );
            byte[] payload = data.toString().getBytes();
            oStream.write( payload );

        } catch ( IOException e ) {
            e.printStackTrace();
            Logger.debug( "ShaveService.sendMessage(): Killing ourself.." );
            android.os.Process.killProcess( android.os.Process.myPid() );
        }

    }

    private class MessageListener extends AsyncTask<InputStream, byte[], Void> {
        private byte[] reply = new byte[ 8000 ];

        @Override
        protected Void doInBackground( InputStream... params ) {
            if ( params[ 0 ] == null ) {
                Logger.debug( "yes is null" );
            }
            while ( true ) {
                try {
                    // Logger.info( "gonna wait in asynctask.." );
                    params[ 0 ].read( reply );
                } catch ( IOException e ) {
                    e.printStackTrace();
                }
                publishProgress( reply );
            }
        }

        @Override
        protected void onProgressUpdate( byte[]... values ) {
            dealWithReply( values[ 0 ] );
        }

    }

    private void dealWithReply( byte[] reply ) {
        try {
            JSONObject data = new JSONObject( new String( reply ) );
            String packetType = data.getString( "packet_type" );
            long incomingFileSize;
            String incomingFilePath, uploaderUsername, downloaderUsername, unknownRecipient;
            String outgoingFilePath;
            if ( packetType.equals( "file_push_req" ) ) {
                Logger.info( "recvd packet is = " + data.toString() );
                incomingFileSize = data.getLong( "file_size" );
                incomingFilePath = data.getString( "file_name" );
                uploaderUsername = data.getString( "uploader_username" );
                Logger.info( "file_push_req recvd from = " + uploaderUsername + ", filename = " + incomingFilePath );
                replyYesToUploader( uploaderUsername, incomingFilePath );
                // TODO: implement UI for request.
                new Downloader( incomingFilePath, incomingFileSize, uploaderUsername ).execute();

            } else if ( packetType.equals( "recipient_not_found" ) ) {
                Logger.info( "ShaveService.dealWithReply: recipient_not_found received" );
                unknownRecipient = data.getString( "unknown_recipient" );
                Intent intent = new Intent( Definitions.INTENT_RECIPIENT_NOT_FOUND );
                intent.putExtra( "unknown_recipient", unknownRecipient );
                this.sendBroadcast( intent );

            }

            else if ( packetType.equals( "file_push_req_ack" ) ) {
                // tell the server we're gonna start uploading
                Logger.info( "dealWithReply: all's well, file_push_req_ack received" );
                outgoingFilePath = data.getString( "file_path" );
                Toast.makeText( activityContext, "Starting to upload: " + outgoingFilePath, Toast.LENGTH_LONG ).show();

                new Uploader( outgoingFilePath ).execute();
            }

            else if ( packetType.equals( "status_req" ) ) {
                // for now, reply saying 'online'
                Logger.info( "dealWithReply: request_status received, replying - online" );
                replyWithStatus( "online" );
            }

            else if ( packetType.equals( "receivers_status_ack" ) ) {
                Logger.info( "dealWithReply: receivers_status_ack recvd. here's the list = " + data.getString( "users_status" ) );
                notifyUser( data.getString( "users_status" ) );
            }

        } catch ( JSONException e ) {
            e.printStackTrace();
        } catch ( Exception e ) {
            e.printStackTrace();
            Logger.error( "ShaveService.dealWithReply: other generic exception, killing ourselves" );
        }
    }

    private void notifyUser( String availableUsers ) {
        Intent intent = new Intent( Definitions.INTENT_AVAILABLE_USERS );
        intent.putExtra( "available_users", availableUsers );
        this.sendBroadcast( intent );
    }

    private void replyWithStatus( String status ) {
        JSONObject data = new JSONObject();
        try {
            data.put( "packet_type", "status_ack" );
            data.put( "username", getOurUserName() );
            data.put( "statuss", status );

            sendMessage( data );
        } catch ( JSONException e ) {
            e.printStackTrace();
        }
    }

    private void replyYesToUploader( String uploaderUsername, String incomingFilePath ) {
        JSONObject data = new JSONObject();
        try {
            data.put( "packet_type", "file_push_req_ack" );
            data.put( "username", getOurUserName() );
            data.put( "uploader_username", uploaderUsername );
            data.put( "file_path", incomingFilePath );
            data.put( "to", uploaderUsername );

            sendMessage( data );
        } catch ( JSONException e ) {
            e.printStackTrace();
        }

    }

    public String getOurUserName() {

        SharedPreferences settings = getSharedPreferences( Definitions.credsPrefFile, Context.MODE_PRIVATE );
        Definitions.OUR_USERNAME = settings.getString( Definitions.prefUserName, Definitions.defaultUserName );
        // TODO: for now, we're using this static field, could do away with it,
        // or improve?
        return Definitions.OUR_USERNAME;
    }

    public void takeActivityContext( Context context ) {
        activityContext = context;
    }
}
