package com.tejus.shavedoggery.util;

import android.util.Log;

public class Logger {
    private static String TAG = "xxxxShaveDoggery..";

    public static void info( String params ) {
        Log.i( TAG, params );
    }

    public static void debug( String params ) {
        Log.d( TAG, params );
    }

    public static void error( String params ) {
        Log.e( TAG, params );
    }

    public static void trace( String params ) {
        Log.e( TAG, params );

    }
}
