package com.tejus.shavedoggery.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;

import org.json.JSONException;
import org.json.JSONObject;

import com.tejus.shavedoggery.R;
import com.tejus.shavedoggery.core.Definitions;
import com.tejus.shavedoggery.core.ShaveService;
import com.tejus.shavedoggery.util.Logger;
import com.tejus.shavedoggery.util.ShaveDialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class WelcomeActivity extends ListActivity implements OnSharedPreferenceChangeListener {

    String sdcardDir = Environment.getExternalStorageDirectory().toString();
    ArrayList<String> rootListing = new ArrayList<String>();
    String mCurrentDirectory;
    ArrayList<String> mFiles = new ArrayList<String>();
    HashMap<String, String> mFileLengthMap = new HashMap<String, String>();

    Button backButton, homeButton, refreshButton;
    Context mContext;

    ServiceConnection mConnection;
    ShaveService mShaveService;
    BroadcastReceiver mShaveReceiver = new ServiceIntentReceiver();
    Handler handler = new Handler();
    SharedPreferences mPrefs;
    private ProgressDialog searchProgressDialog;

    @Override
    public void onCreate( Bundle args ) {
        super.onCreate( args );
        initShaveServiceStuff();
        mContext = this;
        setContentView( R.layout.welcome_layout );
        initReceiver();
        mPrefs = mContext.getSharedPreferences( Definitions.credsPrefFile, Context.MODE_PRIVATE );

        checkUserName();

        refreshButton = ( Button ) findViewById( R.id.refresh );
        homeButton = ( Button ) findViewById( R.id.home );
        backButton = ( Button ) findViewById( R.id.back );

        homeButton.setVisibility( View.GONE );
        // backButton.setVisibility( View.GONE );

        refreshButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View v ) {
                startActivity( ( new Intent().setClass( mContext, WelcomeActivity.class ).setFlags( Intent.FLAG_ACTIVITY_NEW_TASK ).putExtra( "directory_path",
                        mCurrentDirectory ) ) );
            }
        } );

        // handler.postDelayed( new Runnable() {
        // @Override
        // public void run() {
        // sendBootup();
        // }
        // }, 2000 );
        Bundle bundle = getIntent().getExtras();
        if ( bundle != null ) {
            mCurrentDirectory = ( String ) bundle.get( "directory_path" );
            Logger.debug( "WelcomeActivity.onCreate: mCurrentDirectory = " + mCurrentDirectory );
        } else {
            mCurrentDirectory = "";
        }
        try {
            mFiles = new SdCardLister( mCurrentDirectory ).execute().get();
            Logger.info( "oncreate: mFiles = " + mFiles.toString() );
            MySimpleArrayAdapter adapter = new MySimpleArrayAdapter( this, mFiles );
            setListAdapter( adapter );

        } catch ( InterruptedException e ) {
            e.printStackTrace();
        } catch ( ExecutionException e ) {
            e.printStackTrace();
        }

        searchProgressDialog = new ProgressDialog( this );
        searchProgressDialog.setMessage( getResources().getString( R.string.searching_peers_message ) );
        searchProgressDialog.setCancelable( false );

    }

    @Override
    public void onResume() {
        super.onResume();
        mPrefs.registerOnSharedPreferenceChangeListener( this );
    }

    @Override
    public void onPause() {
        super.onPause();
        mPrefs.unregisterOnSharedPreferenceChangeListener( this );
        handler.postDelayed( new Runnable() {
            @Override
            public void run() {
                if ( !mShaveService.getOurUserName().equals( Definitions.defaultUserName ) ) {
                    sendBootup();
                    // TODO: I don't like this:
                    mShaveService.takeActivityContext( mContext );
                }
            }
        }, 2000 );
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

            case R.id.test_api1:
                this.testApi1();
                return true;

            case R.id.quit:
                this.quit();
                return true;

            default:
                return super.onOptionsItemSelected( item );
        }
    }

    private class ServiceIntentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive( Context context, Intent intent ) {
            String action = intent.getAction();
            Logger.info( "WelcomeActivity.ServiceIntentReceiver: action received = " + action );
            if ( action.equals( Definitions.INTENT_RECIPIENT_NOT_FOUND ) ) {
                showRecipientNotFoundToast( intent );
            } else if ( action.equals( Definitions.INTENT_AVAILABLE_USERS ) ) {
                showAvailableUsers( intent );
            }
        }

        private void showAvailableUsers( Intent intent ) {
            ArrayList<String> listUsers = new ArrayList<String>();
            String tempElement;
            String list = ( intent.getStringExtra( "available_users" ) != null ) ? intent.getStringExtra( "available_users" ) : null;

            Logger.debug( "recvd list = " + list );

            if ( searchProgressDialog != null && searchProgressDialog.isShowing() ) {
                searchProgressDialog.dismiss();
            }

            StringTokenizer strTok = new StringTokenizer( list, "," );
            while ( strTok.hasMoreTokens() ) {
                tempElement = strTok.nextToken();
                if ( !listUsers.contains( cleanString( tempElement ) ) && !cleanString( tempElement ).equals( mShaveService.getOurUserName() ) ) {
                    listUsers.add( cleanString( tempElement ) );
                }
            }

            final ArrayList<String> finalList = listUsers;
            Logger.info( "and fileToPush here = " + Definitions.fileToPush );

            AlertDialog.Builder builder = new AlertDialog.Builder( mContext );
            builder.setTitle( getFileNameTrivial( Definitions.fileToPush ) + ": " + getResources().getString( R.string.send_question ) );

            Logger.debug( "charseq = " + listUsers.toArray( new CharSequence[ listUsers.size() ] ) );

            builder.setItems( listUsers.toArray( new CharSequence[ listUsers.size() ] ), new DialogInterface.OnClickListener() {
                public void onClick( DialogInterface dialog, int item ) {
                    JSONObject data = new JSONObject();
                    try {
                        data.put( "packet_type", "file_push_req" );
                        data.put( "file_size", getFileSize( Definitions.fileToPush ) );
                        data.put( "file_name", Definitions.fileToPush );
                        data.put( "to", finalList.get( item ) );
                        data.put( "username", mShaveService.getOurUserName() );

                        mShaveService.sendMessage( data );
                    } catch ( JSONException e ) {
                        e.printStackTrace();
                    }
                }
            } );
            builder.create().show();
        }

        private void showRecipientNotFoundToast( Intent intent ) {
            String unknownRecipient = ( intent.getStringExtra( "unknown_recipient" ) != null ) ? intent.getStringExtra( "unknown_recipient" ) : null;
            Toast.makeText( mContext, getResources().getString( R.string.unknown_recipient ) + " " + unknownRecipient, Toast.LENGTH_LONG ).show();

        }

    }

    private void testApi() {
        Socket testSocket;
        FileInputStream testIStream;
        OutputStream testOStream;
        byte[] writeArray = new byte[ 1024 * 1024 ];
        try {
            testSocket = new Socket( "23.21.239.205", 60000 );

            testIStream = new FileInputStream( new File( "/mnt/sdcard/Eagles of Death Metal - Peace Love Death Metal 17 Just Nineteen.mp3" ) );
            testOStream = ( OutputStream ) testSocket.getOutputStream();

            while ( testIStream.read( writeArray ) > 0 ) {
                Logger.info( "gonna write test file chunk.." );
                testOStream.write( writeArray );
            }
            testIStream.close();
            testOStream.close();
            testSocket.close();

            Logger.debug( "test Uploader done uploading file  " );

        } catch ( UnknownHostException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch ( IOException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // JSONObject data = new JSONObject();
        // try {
        // data.put( "username", Definitions.OUR_USERNAME );
        // data.put( "packet_type", "bootup" );
        // mShaveService.sendMessage( data );
        // } catch ( JSONException e ) {
        // e.printStackTrace();
        // }

    }

    void testApi1() {
        Socket testReceiverSocket;
        try {
            testReceiverSocket = new Socket( Definitions.SERVER_IP, 61000 );
            InputStream iStream = testReceiverSocket.getInputStream();
            FileOutputStream fileStream = new FileOutputStream( Environment.getExternalStorageDirectory().toString() + "/testDownload" );
            byte[] testReadArray = new byte[ Definitions.WRITE_BUFFER_SIZE ];
            long testFileProgressSize = 0;
            int size;

            while ( ( size = iStream.read( testReadArray ) ) > 0 ) {
                testFileProgressSize += size;
                Logger.info( "TestDownloader.doInBackground(): writing chunk - " + testFileProgressSize + " B done.. " );
                fileStream.write( testReadArray, 0, size );
            }

            testReceiverSocket.close();
            fileStream.close();
            iStream.close();

        } catch ( UnknownHostException e ) {
            e.printStackTrace();
        } catch ( IOException e ) {
            e.printStackTrace();
        }

    }

    private void checkUserName() {

        // userName.exists? nothing : get one
        SharedPreferences settings = getSharedPreferences( Definitions.credsPrefFile, Context.MODE_PRIVATE );
        if ( settings.getString( Definitions.prefUserName, "" ).length() < 3 ) {
            ShaveDialog dialog = new ShaveDialog( this, getResources().getString( R.string.user_name_dialog_title ), getResources().getString(
                    R.string.user_name_dialog_message ), getResources().getString( R.string.ok ) );
        }
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

    private void initReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction( Definitions.INTENT_RECIPIENT_NOT_FOUND );
        filter.addAction( Definitions.INTENT_AVAILABLE_USERS );
        registerReceiver( mShaveReceiver, filter );
    }

    private void sendBootup() {
        JSONObject data = new JSONObject();
        try {
            data.put( "username", mShaveService.getOurUserName() );
            data.put( "packet_type", "bootup" );
            mShaveService.sendMessage( data );
        } catch ( JSONException e ) {
            e.printStackTrace();
        }
    }

    public class MySimpleArrayAdapter extends ArrayAdapter<String> {
        private final Activity context;
        private final ArrayList<String> fileList;

        public MySimpleArrayAdapter( Activity context, ArrayList<String> fileList ) {
            super( context, R.layout.file_row, fileList );
            this.context = context;
            this.fileList = fileList;
        }

        @Override
        public View getView( int position, View convertView, ViewGroup parent ) {
            String name = null;

            LayoutInflater inflater = context.getLayoutInflater();
            View rowView = inflater.inflate( R.layout.file_row, null, true );
            TextView itemName = ( TextView ) rowView.findViewById( R.id.item_name );
            TextView itemSize = ( TextView ) rowView.findViewById( R.id.item_size );
            if ( fileList.size() > 0 ) {
                if ( fileList.get( position ).startsWith( "#" ) ) {
                    // Logger.info(
                    // "WelcomeActivity.getView(): setting bold for position = "
                    // + position );
                    itemName.setTypeface( null, Typeface.BOLD );
                    itemSize.setVisibility( View.GONE );
                    name = getFileNameTrivial( fileList.get( position ) );
                } else {
                    // Log.d( "XXXX", "setting italics for position = " +
                    // position );
                    itemName.setTypeface( null, Typeface.ITALIC );
                    if ( mFileLengthMap.get( mFiles.get( position ) ) != null ) {
                        itemSize.setText( saneFileSizeRepresentation( mFileLengthMap.get( mFiles.get( position ) ) ) );
                    }
                    name = getFileNameTrivial( fileList.get( position ) );
                }
                itemName.setText( name );
            }
            return rowView;
        }
    }

    @Override
    protected void onListItemClick( ListView l, View v, int position, long id ) {

        Logger.debug( "WelcomeActivity.onListItemClick: item clicked = " + mFiles.get( position ) );
        String filePath = mFiles.get( position ); // if it's a file, fire up the
        if ( isNotADirectory( filePath ) ) {
            Logger.info( " gonna  send receivers_status_req" );

            JSONObject data = new JSONObject();
            try {
                data.put( "packet_type", "receivers_status_req" );
                data.put( "username", mShaveService.getOurUserName() );

                mShaveService.sendMessage( data );
            } catch ( JSONException e ) {
                e.printStackTrace();
            }

            // sendRequestAlert( filePath.replace( "#", "" ) );
            Definitions.fileToPush = filePath.replace( "#", "" );
            Logger.info( "fileToPush here = " + Definitions.fileToPush );
            searchProgressDialog.show();

        } else {
            startActivity( ( new Intent().setClass( this, WelcomeActivity.class ).setFlags( Intent.FLAG_ACTIVITY_NEW_TASK ).putExtra( "directory_path",
                    filePath.replace( "#", "" ) ) ) );
        }

    }

    private void sendRequestAlert( String filePath ) {
        final EditText input = new EditText( this );
        final String finalFilePath = filePath;
        new AlertDialog.Builder( mContext ).setIcon( R.drawable.iconshave )
                .setTitle( getFileNameTrivial( filePath ) )
                .setMessage( R.string.send_question )
                .setView( input )
                .setPositiveButton( R.string.send, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick( DialogInterface arg0, int arg1 ) {
                        String recipient = input.getText().toString().toLowerCase();
                        JSONObject data = new JSONObject();
                        try {
                            data.put( "packet_type", "file_push_req" );
                            data.put( "file_size", getFileSize( finalFilePath ) );
                            data.put( "file_name", finalFilePath );
                            data.put( "to", recipient );
                            data.put( "username", mShaveService.getOurUserName() );

                            mShaveService.sendMessage( data );
                        } catch ( JSONException e ) {
                            e.printStackTrace();
                        }
                    }

                } )
                .create()
                .show();
    }

    private class SdCardLister extends AsyncTask<Void, Void, ArrayList<String>> {
        String directoryName;

        public SdCardLister( String directoryName ) {
            this.directoryName = directoryName;
        }

        @Override
        protected ArrayList<String> doInBackground( Void... params ) {
            ArrayList<String> files = new ArrayList<String>();
            File file[] = null;
            if ( directoryName.length() > 0 ) {
                Logger.info( "SdCardLister: searching directory - " + directoryName );
                file = new File( directoryName ).listFiles();
            } else {
                Logger.info( "SdCardLister: searching directory - " + sdcardDir );
                file = new File( sdcardDir ).listFiles();
            }
            // if the folder isn't empty:
            if ( file != null ) {
                for ( File iFile : file ) {
                    if ( iFile.isDirectory() ) {
                        files.add( "#" + iFile.getAbsolutePath() );
                    } else {
                        files.add( iFile.getAbsolutePath() );
                        mFileLengthMap.put( iFile.getAbsolutePath(), String.valueOf( iFile.length() ) );
                        Logger.info( "mFileLengthMap here = " + mFileLengthMap.toString() );
                    }
                }
            } else {
                // the folder's empty
                files.add( "" );
            }
            return files;
        }

    }

    private String getFileNameTrivial( String filePath ) {
        return filePath.substring( filePath.lastIndexOf( "/" ) + 1 );
    }

    String saneFileSizeRepresentation( String fileSize ) {
        // Log.d( "XXXX", "saneFileSizeRepresentation called for - " + fileSize
        // );
        fileSize = fileSize.replace( "]", "" ).replace( "[", "" );
        if ( fileSize.length() < 4 ) {
            return fileSize + " B";
        } else if ( fileSize.length() < 7 ) {
            return String.valueOf( ( Long.parseLong( fileSize ) / 1000 ) ) + " KB";
        } else if ( fileSize.length() < 10 ) {
            // Log.d( "XXXX", "for MB: " + ( Long.parseLong( fileSize ) /
            // 1000000 ) );
            return String.valueOf( ( Long.parseLong( fileSize ) / ( long ) 1000000 ) ) + " MB";
        } else if ( fileSize.length() < 13 ) {
            return String.valueOf( ( Long.parseLong( fileSize ) / 1000000000 ) ) + " GB";
        } else {
            return null;
        }

    }

    private boolean isNotADirectory( String filePath ) {
        return !( filePath.trim().startsWith( "#" ) );
    }

    private long getFileSize( String filePath ) {
        File tempFile = new File( filePath );
        if ( tempFile.exists() ) {
            return tempFile.length();
        } else {
            return -1;
        }

    }

    private void quit() {

        Logger.debug( "WelcomeActivity.quit(): Killing ourself.." );
        android.os.Process.killProcess( android.os.Process.myPid() );

    }

    @Override
    public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String key ) {

        Logger.debug( "Username changed to = " + sharedPreferences.getString( key, Definitions.defaultUserName ) );
        sendBootup();

    }

    String cleanString( String dirty ) {
        return dirty.replace( "[", "" ).replace( "]", "" ).replace( "\"", "" );
    }
}
