package com.tejus.shavedoggery.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream.GetField;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import org.json.JSONException;
import org.json.JSONObject;

import com.tejus.shavedoggery.util.Logger;

import android.os.AsyncTask;
import android.os.Environment;

public class Downloader extends AsyncTask<Void, Void, Void> {

    String mFileName, mUploaderUsername;
    long mFileSize, fileProgressSize = 0;
    Socket mSocket;
    OutputStream oStream;
    InputStream iStream;
    File downloadLocation;
    FileOutputStream fileStream;
    int size;
    byte[] readArray = new byte[ Definitions.WRITE_BUFFER_SIZE ];

    public Downloader( String filePath, long fileSize, String uploaderUsername ) {
        mFileName = filePath;
        mFileSize = fileSize;
        mUploaderUsername = uploaderUsername;
        downloadLocation = new File( Environment.getExternalStorageDirectory().toString() + "/" + getFileNameTrivial( filePath ) );
        try {
            mSocket = new Socket( Definitions.SERVER_IP, Definitions.SERVER_UPLOAD_PORT );
            oStream = mSocket.getOutputStream();
            iStream = mSocket.getInputStream();
            fileStream = new FileOutputStream( downloadLocation );
            JSONObject data = new JSONObject();
            data.put( "packet_type", "download_stream" );
            data.put( "username", Definitions.OUR_USERNAME );
            data.put( "uploader_username", mUploaderUsername );
            byte[] payload = data.toString().getBytes();
            oStream.write( payload );
        } catch ( UnknownHostException e ) {
            e.printStackTrace();
        } catch ( IOException e ) {
            e.printStackTrace();
        } catch ( JSONException e ) {
            e.printStackTrace();
        }
    }

    @Override
    protected Void doInBackground( Void... arg0 ) {
        try {
            while ( ( size = iStream.read( readArray ) ) > 0 ) {
                fileProgressSize += size;
                Logger.info( "Downloader.doInBackground(): writing chunk - " + fileProgressSize + " B done.. " );
                fileStream.write( readArray, 0, size );
            }

            mSocket.close();
            oStream.close();
            iStream.close();
            fileStream.close();

            Logger.info( "Downloader.doInBackground(): done downloading file - " + mFileName );

        } catch ( IOException e ) {
            e.printStackTrace();
        }
        return null;
    }

    private String getFileNameTrivial( String filePath ) {
        return filePath.substring( filePath.lastIndexOf( "/" ) + 1 );
    }
}
