package com.tejus.shavedoggery.ui;

import java.io.IOException;

import android.content.Context;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class CameraView extends View implements SurfaceHolder.Callback, View.OnTouchListener {

    private AudioManager mAudioManager = null;
    private Camera mCamera = null;
    private MediaRecorder mMediaRecorder = null;
    private SurfaceHolder mCameraSurfaceHolder;
    private SurfaceView mCameraSurfaceView;

    public CameraView( Context context ) {
        super( context );
    }

    public CameraView( Context context, AttributeSet attr ) {
        super( context, attr );

        mAudioManager = ( AudioManager ) context.getSystemService( Context.AUDIO_SERVICE );
        mAudioManager.setStreamMute( AudioManager.STREAM_SYSTEM, true );
    }

    @Override
    public boolean onTouch( View v, MotionEvent event ) {
        return true;
    }

    @Override
    public void surfaceChanged( SurfaceHolder holder, int format, int width, int height ) {

        if ( mCamera != null && mMediaRecorder == null ) {
            mCamera.stopPreview();
            try {
                mCamera.setPreviewDisplay( holder );
            } catch ( Exception ex ) {
                ex.printStackTrace();
            }
            mCamera.startPreview();
        }

    }

    @Override
    public void surfaceCreated( SurfaceHolder holder ) {

    }

    @Override
    public void surfaceDestroyed( SurfaceHolder holder ) {

    }

    public void setupCamera( SurfaceView sv ) {
        mCameraSurfaceView = sv;
        mCameraSurfaceHolder = mCameraSurfaceView.getHolder();
        mCameraSurfaceHolder.addCallback( this );
        mCameraSurfaceHolder.setType( SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS );

        mCamera = Camera.open();
        /*
         * Camera.Parameters p = myCamera.getParameters();
         * myCamera.setParameters(p);
         */

        setOnTouchListener( this );
    }

    public void prepareMedia( int targetWidth, int targetHeight ) {

        mMediaRecorder = new MediaRecorder();
        mCamera.stopPreview();
        mCamera.unlock();

        mMediaRecorder.setCamera( mCamera );
        mMediaRecorder.setAudioSource( MediaRecorder.AudioSource.CAMCORDER );
        mMediaRecorder.setVideoSource( MediaRecorder.VideoSource.CAMERA );

        CamcorderProfile targetProfile = CamcorderProfile.get( CamcorderProfile.QUALITY_LOW );
        targetProfile.videoFrameWidth = targetWidth;
        targetProfile.videoFrameHeight = targetHeight;
        targetProfile.videoFrameRate = 25;
        targetProfile.videoBitRate = 512 * 1024;
        targetProfile.videoCodec = MediaRecorder.VideoEncoder.H264;
        targetProfile.audioCodec = MediaRecorder.AudioEncoder.AMR_NB;
        targetProfile.fileFormat = MediaRecorder.OutputFormat.MPEG_4;
        mMediaRecorder.setProfile( targetProfile );

    }

    public boolean startRecording( String targetFile ) {
        mMediaRecorder.setOutputFile( targetFile );

        return reallyStart();
    }

    private boolean reallyStart() {

        mMediaRecorder.setPreviewDisplay( mCameraSurfaceHolder.getSurface() );
        try {
            mMediaRecorder.prepare();
        } catch ( IllegalStateException e ) {
            releaseMediaRecorder();
            Log.d( "TEAONLY", "JAVA:  camera prepare illegal error" );
            return false;
        } catch ( IOException e ) {
            releaseMediaRecorder();
            Log.d( "TEAONLY", "JAVA:  camera prepare io error" );
            return false;
        }

        try {
            mMediaRecorder.start();
        } catch ( Exception e ) {
            releaseMediaRecorder();
            Log.d( "TEAONLY", "JAVA:  camera start error" );
            return false;
        }

        return true;
    }

    private void releaseMediaRecorder() {
        if ( mMediaRecorder != null ) {
            mMediaRecorder.reset(); // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mCamera.lock(); // lock camera for later use
            mCamera.startPreview();
        }
        mMediaRecorder = null;
    }

}
