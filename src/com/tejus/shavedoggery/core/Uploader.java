package com.tejus.shavedoggery.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import org.json.JSONException;
import org.json.JSONObject;

import com.tejus.shavedoggery.util.Logger;

import android.os.AsyncTask;

public class Uploader extends AsyncTask<Void, Void, Void> {

    String mFilePath;
    FileInputStream mIStream;
    OutputStream mOStream;
    Socket mSocket;
    byte[] writeArray = new byte[ Definitions.WRITE_BUFFER_SIZE ];

    public Uploader( String filePath ) {
        mFilePath = filePath;
        try {
            mSocket = new Socket( Definitions.SERVER_IP, Definitions.SERVER_UPLOAD_PORT );
            mIStream = new FileInputStream( new File( mFilePath ) );
            mOStream = ( OutputStream ) mSocket.getOutputStream();
            JSONObject data = new JSONObject();
            data.put( "packet_type", "upload_stream" );
            data.put( "username", Definitions.OUR_USERNAME );
            byte[] payload = data.toString().getBytes();
            mOStream.write( payload );

        } catch ( UnknownHostException e ) {
            e.printStackTrace();
        } catch ( IOException e ) {
            e.printStackTrace();
        } catch ( JSONException e ) {
            e.printStackTrace();
        }
    }

    @Override
    protected Void doInBackground( Void... params ) {
        try {
            while ( mIStream.read( writeArray ) > 0 ) {
                Logger.info( "gonna write file chunk.." );
                mOStream.write( writeArray );
            }
            mIStream.close();
            mOStream.close();
            mSocket.close();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        Logger.debug( "Uploader.doInBackground: done uploading file = " + mFilePath );

        return null;
    }

}
