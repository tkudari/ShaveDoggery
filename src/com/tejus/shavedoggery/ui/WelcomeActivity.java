package com.tejus.shavedoggery.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;

import com.tejus.shavedoggery.R;
import com.tejus.shavedoggery.util.Logger;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class WelcomeActivity extends ListActivity {

    String sdcardDir = Environment.getExternalStorageDirectory().toString();
    ArrayList<String> rootListing = new ArrayList<String>();
    String mCurrentDirectory;
    ArrayList<String> mFiles = new ArrayList<String>();
    HashMap<String, String> mFileLengthMap = new HashMap<String, String>();

    @Override
    public void onCreate( Bundle args ) {
        super.onCreate( args );
        setContentView( R.layout.welcome_layout );
        Bundle bundle = getIntent().getExtras();
        if ( bundle != null ) {
            mCurrentDirectory = ( String ) bundle.get( "directory_path" );
            Logger.debug( "WelcomeActivity.onCreate: mCurrentDirectory = " + mCurrentDirectory );
        } else {
            mCurrentDirectory = "";
        }
        try {
            mFiles = new SdCardLister( "" ).execute().get();
            Logger.info( "oncreate: mFiles = " + mFiles.toString() );
            MySimpleArrayAdapter adapter = new MySimpleArrayAdapter( this, mFiles );
            setListAdapter( adapter );

        } catch ( InterruptedException e ) {
            e.printStackTrace();
        } catch ( ExecutionException e ) {
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
                    Logger.info( "WelcomeActivity.getView(): setting bold for position = " + position );
                    itemName.setTypeface( null, Typeface.BOLD );
                    itemSize.setVisibility( View.GONE );
                    name = getFileNameTrivial( fileList.get( position ) );
                } else {
                    Log.d( "XXXX", "setting italics for position = " + position );
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
            // sendRequestAlert(filePath);
        } else {
            startActivity( ( new Intent().setClass( this, WelcomeActivity.class ).setFlags( Intent.FLAG_ACTIVITY_NEW_TASK ).putExtra( "directory_path",
                    filePath ) ) );
        }

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
        Log.d( "XXXX", "saneFileSizeRepresentation called for - " + fileSize );
        fileSize = fileSize.replace( "]", "" ).replace( "[", "" );
        if ( fileSize.length() < 4 ) {
            return fileSize + " B";
        } else if ( fileSize.length() < 7 ) {
            return String.valueOf( ( Long.parseLong( fileSize ) / 1000 ) ) + " KB";
        } else if ( fileSize.length() < 10 ) {
            Log.d( "XXXX", "for MB: " + ( Long.parseLong( fileSize ) / 1000000 ) );
            return String.valueOf( ( Long.parseLong( fileSize ) / ( long ) 1000000 ) ) + " MB";
        } else if ( fileSize.length() < 13 ) {
            return String.valueOf( ( Long.parseLong( fileSize ) / 1000000000 ) ) + " GB";
        } else {
            return null;
        }

    }

    // sets the current directory & populates mFiles & mFileLengthMap
    private void processListing( String string ) {
        int index = 0;
        String word;
        String fileList = null;
        String[] currentDirFinder = new String[ 2 ];
        // find the current directory:
        StringTokenizer dirTok = new StringTokenizer( string, "$" );
        for ( int i = 0; ( dirTok.hasMoreTokens() && i <= 1 ); i++ ) {
            currentDirFinder[ i ] = dirTok.nextToken();
        }
        if ( currentDirFinder[ 0 ].length() > 0 ) {
            fileList = currentDirFinder[ 0 ].replace( "[", "" ).replace( "]", "" );
            // populate mFiles:
            StringTokenizer strTok = new StringTokenizer( fileList, "," );
            while ( strTok.hasMoreTokens() ) {
                word = strTok.nextToken();

                // populate file sizes (mFileLengthMap):
                StringTokenizer lengthTok = new StringTokenizer( word, "^" );
                String[] fileLengthFinder = new String[ 2 ];
                for ( int i = 0; lengthTok.hasMoreTokens(); i++ ) {
                    fileLengthFinder[ i ] = lengthTok.nextToken();
                }
                mFiles.add( word.replace( " ", "" ).replace( "[", "" ).replace( "]", "" ) );
                Log.d( "XXXX", "file added = " + mFiles.get( index ) );
                if ( fileLengthFinder[ 1 ] != null ) {
                    mFileLengthMap.put( mFiles.get( index ), fileLengthFinder[ 1 ] );
                }
                ++index;
            }
        }
        mCurrentDirectory = currentDirFinder[ 1 ];

        Log.d( "XXXX", "mFiles length = " + mFiles.size() );
        Log.d( "XXXX", "mFileLengthMap = " + mFileLengthMap.toString() );
        Log.d( "XXXX", "mCurrentDirectory = " + mCurrentDirectory );

    }

    private String stripLengthOff( String filePath ) {
        Log.d( "XXXX", "filePath in stripLengthOff = " + filePath );
        Log.d( "XXXX", "lastindex in stripLengthOff = " + filePath.lastIndexOf( "^" ) );
        if ( filePath.contains( "^" ) ) {
            return filePath.substring( 0, filePath.lastIndexOf( "^" ) );
        } else {
            return null;
        }
    }

    private boolean isNotADirectory( String filePath ) {
        return !( filePath.trim().startsWith( "#" ) );
    }

}
