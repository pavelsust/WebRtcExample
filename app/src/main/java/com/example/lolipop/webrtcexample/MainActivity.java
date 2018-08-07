package com.example.lolipop.webrtcexample;

import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Capturer;
import org.webrtc.Camera1Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = MainActivity.class.getSimpleName();
    PeerConnectionFactory peerConnectionFactory;
    private MediaConstraints sdpConstraints;
    private VideoSource videoSource;
    private VideoTrack localVideoTrack;
    private AudioSource audioSource;
    private AudioTrack localAudioTrack;


    private SurfaceViewRenderer localVideoView;
    private SurfaceViewRenderer remoteVideoView;
    private VideoRenderer localRenderer;
    private VideoRenderer remoteRenderer;

    private PeerConnection localPeer , remotePeer;
    private Button start , call , hangup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initViews();
        initVideos();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }


    /**
     *
     *  Initialized the main view off application
     *  Like camera , video and button
     */

    private void initViews() {
        start = (Button) findViewById(R.id.start_call);
        call = (Button) findViewById(R.id.init_call);
        hangup = (Button) findViewById(R.id.end_call);
        localVideoView = (SurfaceViewRenderer) findViewById(R.id.local_gl_surface_view);
        remoteVideoView = (SurfaceViewRenderer) findViewById(R.id.remote_gl_surface_view);

        start.setOnClickListener(this);
        call.setOnClickListener(this);
        hangup.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_call: {
                start();
                break;
            }
            case R.id.init_call: {
                call();
                break;
            }
            case R.id.end_call: {
                hangup();
                break;
            }
        }
    }




    public void start(){
        //Initialize PeerConnectionFactory globals.
        //Params are context, initAudio,initVideo and videoCodecHwAcceleration
        //PeerConnectionFactory.initializeAndroidGlobals(this, true, true, true);
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(this).setEnableVideoHwAcceleration(true).createInitializationOptions());


        //Create a new PeerConnectionFactory instance.
        //PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        PeerConnectionFactory peerConnectionFactory = PeerConnectionFactory.builder().createPeerConnectionFactory();


        //Now create a VideoCapturer instance. Callback methods are there if you want to do something! Duh!
        VideoCapturer videoCapturerAndroid = getVideoCapture();
        //Create MediaConstraints - Will be useful for specifying video and audio constraints. More on this later!
        MediaConstraints constraints = new MediaConstraints();

        //Create a VideoSource instance
        VideoSource videoSource = peerConnectionFactory.createVideoSource(videoCapturerAndroid);
        VideoTrack localVideoTrack = peerConnectionFactory.createVideoTrack("100", videoSource);

        //create an AudioSource instance
        AudioSource audioSource = peerConnectionFactory.createAudioSource(constraints);
        AudioTrack localAudioTrack = peerConnectionFactory.createAudioTrack("101", audioSource);

        //we will start capturing the video from the camera
        //width,height and fps
        videoCapturerAndroid.startCapture(1000, 1000, 30);

        //create surface renderer, init it and add the renderer to the track
        SurfaceViewRenderer videoView = (SurfaceViewRenderer) findViewById(R.id.local_gl_surface_view);
        videoView.setMirror(true);

        EglBase rootEglBase = EglBase.create();
        videoView.init(rootEglBase.getEglBaseContext(), null);

        localVideoTrack.addRenderer(new VideoRenderer(videoView));


    }




    /**
     *
     * Initialize this video view
     */
    private void initVideos(){
        EglBase rootEglBase = EglBase.create();
        localVideoView.init(rootEglBase.getEglBaseContext() , null);
        remoteVideoView.init(rootEglBase.getEglBaseContext() , null);
        localVideoView.setZOrderMediaOverlay(true);
        remoteVideoView.setZOrderMediaOverlay(true);
    }


    /**
     *
     *  This video capture is deprecated now
     *  so trying to another option for video capture.
     *
     * @return
     */
    private VideoCapturer getVideoCapture() {
        VideoCapturer capturer = createVideoCapturer();
        return capturer;
    }


    /**
     *
     * Create video capture
     * I am using this because in getVideoCapture() videoCaptureAndroid is deprecated now.
     *
     * @return
     */

    private VideoCapturer createVideoCapturer() {
        VideoCapturer videoCapturer = null;
        Log.d(TAG, "Creating capturer using camera1 API.");
        videoCapturer = createCameraCapturer(new Camera1Enumerator(false));
        videoCapturer.startCapture(0 , 90 , 270);
        return videoCapturer;
    }


    /**
     *
     *
     *  This will make Camera Capture
     *
     * @param enumerator
     * @return
     */

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        Log.d(TAG, "Looking for front facing cameras.");
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Log.d(TAG, "Creating front facing camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        Log.d(TAG, "Looking for other cameras.");
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Log.d(TAG, "Creating other camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }




    private void call(){
        start.setEnabled(false);
        call.setEnabled(false);
        hangup.setEnabled(true);
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        sdpConstraints = new MediaConstraints();
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"));
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"));



        // create localPeer
        //creating localPeer
        localPeer = peerConnectionFactory.createPeerConnection(iceServers, sdpConstraints, new CustomPeerConnectionObserver("localPeerCreation") {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                onIceCandidateReceived(localPeer, iceCandidate);
            }
        });



        // create remotePeer
        //creating remotePeer
        remotePeer = peerConnectionFactory.createPeerConnection(iceServers, sdpConstraints, new CustomPeerConnectionObserver("remotePeerCreation") {

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                onIceCandidateReceived(remotePeer, iceCandidate);
            }

            public void onAddStream(MediaStream mediaStream) {
                super.onAddStream(mediaStream);
                gotRemoteStream(mediaStream);
            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                super.onIceGatheringChange(iceGatheringState);

            }
        });

        //creating local mediastream
        MediaStream stream = peerConnectionFactory.createLocalMediaStream("102");
        stream.addTrack(localAudioTrack);
        stream.addTrack(localVideoTrack);
        localPeer.addStream(stream);

        // create offer
        //creating Offer
        localPeer.createOffer(new CustomSdpObserver("localCreateOffer"){
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                //we have localOffer. Set it as local desc for localpeer and remote desc for remote peer.
                //try to create answer from the remote peer.
                super.onCreateSuccess(sessionDescription);
                localPeer.setLocalDescription(new CustomSdpObserver("localSetLocalDesc"), sessionDescription);
                remotePeer.setRemoteDescription(new CustomSdpObserver("remoteSetRemoteDesc"), sessionDescription);
                remotePeer.createAnswer(new CustomSdpObserver("remoteCreateOffer") {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {
                        //remote answer generated. Now set it as local desc for remote peer and remote desc for local peer.
                        super.onCreateSuccess(sessionDescription);
                        remotePeer.setLocalDescription(new CustomSdpObserver("remoteSetLocalDesc"), sessionDescription);
                        localPeer.setRemoteDescription(new CustomSdpObserver("localSetRemoteDesc"), sessionDescription);

                    }
                },new MediaConstraints());
            }
        },sdpConstraints);
    }

    private void hangup() {
        localPeer.close();
        remotePeer.close();
        localPeer = null;
        remotePeer = null;
        start.setEnabled(true);
        call.setEnabled(false);
        hangup.setEnabled(false);
    }


    private void gotRemoteStream(MediaStream stream) {
        //we have remote video stream. add to the renderer.
        final VideoTrack videoTrack = stream.videoTracks.get(0);
        AudioTrack audioTrack = stream.audioTracks.get(0);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    remoteRenderer = new VideoRenderer(remoteVideoView);
                    remoteVideoView.setVisibility(View.VISIBLE);
                    videoTrack.addRenderer(remoteRenderer);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void onIceCandidateReceived(PeerConnection peer, IceCandidate iceCandidate) {
        //we have received ice candidate. We can set it to the other peer.
        if (peer == localPeer) {
            remotePeer.addIceCandidate(iceCandidate);
        } else {
            localPeer.addIceCandidate(iceCandidate);
        }
    }


}
