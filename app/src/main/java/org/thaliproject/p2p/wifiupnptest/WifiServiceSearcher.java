package org.thaliproject.p2p.wifiupnptest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pUpnpServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pUpnpServiceRequest;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION;


/**
 * Created by juksilve on 28.2.2015.
 */

public class WifiServiceSearcher {

    private Context context;
    private BroadcastReceiver receiver;
    private IntentFilter filter;
    private String SERVICE_TYPE;

    private final WifiBase.WifiStatusCallBack callback;
    private WifiP2pManager p2p;
    private WifiP2pManager.Channel channel;
    private WifiP2pManager.UpnpServiceResponseListener serviceListener;
    private WifiP2pManager.PeerListListener peerListListener;

    enum ServiceState{
        NONE,
    //    WaitingTimer,
        DiscoverPeer,
        DiscoverService
    }
    ServiceState myServiceState = ServiceState.NONE;


    List<ServiceItem> myServiceList = new ArrayList<ServiceItem>();

  /*  CountDownTimer PeerDiscoveryTimeOutTimer = new CountDownTimer(90000, 100000) {
        public void onTick(long millisUntilFinished) {
            // not using
        }
        public void onFinish() {
            if(callback != null) {
                callback.WaitTimeCallback();
            }
            startPeerDiscovery();
        }
    };*/

 /*   CountDownTimer WaitTimeTimeOutTimer = new CountDownTimer(600000, 100000) {
        public void onTick(long millisUntilFinished) {
            // not using
        }
        public void onFinish() {
            if(callback != null) {
                callback.WaitTimeCallback();
            }
            myServiceState = ServiceState.NONE;
            startPeerDiscovery();
        }
    };*/

    CountDownTimer ServiceDiscoveryTimeOutTimer = new CountDownTimer(60000, 1000) {
        public void onTick(long millisUntilFinished) {
            // not using
        }
        public void onFinish() {
            stopDiscovery();
            startPeerDiscovery();
        }
    };

    CountDownTimer peerDiscoveryTimer = null;

    public WifiServiceSearcher(Context Context, WifiP2pManager Manager, WifiP2pManager.Channel Channel, WifiBase.WifiStatusCallBack handler, String serviceType) {
        this.context = Context;
        this.p2p = Manager;
        this.channel = Channel;
        this.callback = handler;
        this.SERVICE_TYPE = serviceType;

        Random ran = new Random(System.currentTimeMillis());

        // if this 4 seconds minimum, then we see this
        // triggering before we got all services
        long millisInFuture = 5000 + (ran.nextInt(5000));

        debug_print("peerDiscoveryTimer timeout value:" + millisInFuture);

        peerDiscoveryTimer = new CountDownTimer(millisInFuture, 1000) {
            public void onTick(long millisUntilFinished) {
                // not using
            }
            public void onFinish() {
                myServiceState = ServiceState.NONE;
                if(callback != null) {
                    callback.gotServicesList(myServiceList);

                    myServiceState = ServiceState.DiscoverPeer;
                    //cancel all other counters, and start our wait cycle
                    ServiceDiscoveryTimeOutTimer.cancel();
                    peerDiscoveryTimer.cancel();
                    stopDiscovery();
                    startPeerDiscovery();
                  //  stopPeerDiscovery();
                   // WaitTimeTimeOutTimer.start();
                }else{
                    startPeerDiscovery();
                }
            }
        };
    }

    public List<ServiceItem> GetServiceList(){
        return  myServiceList;
    }

    public void Start() {

        receiver = new ServiceSearcherReceiver();
        filter = new IntentFilter();
        filter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        filter.addAction(WIFI_P2P_PEERS_CHANGED_ACTION);

        this.context.registerReceiver(receiver, filter);

        peerListListener = new WifiP2pManager.PeerListListener() {

            public void onPeersAvailable(WifiP2pDeviceList peers) {

                final WifiP2pDeviceList pers = peers;
                if (pers.getDeviceList().size() > 0) {
                    // this is called still multiple time time-to-time
                    // so need to make sure we only make one service discovery call
                    if(myServiceState != ServiceState.DiscoverService){
//                    && myServiceState != ServiceState.WaitingTimer) {

                        if(callback != null) {
                            callback.gotPeersList(pers.getDeviceList());
                        }
                        //tests have shown that if we have multiple peers with services advertising
                        // who disappear same time when we do this, there is a chance that we get stuck
                        // thus, if this happens, in 60 seconds we'll cancel this query and start peer discovery again
                        ServiceDiscoveryTimeOutTimer.start();
                        startServiceDiscovery();
                    }
                }
            }
        };

        serviceListener = new WifiP2pManager.UpnpServiceResponseListener(){
            @Override
            public void onUpnpServiceAvailable(List<String> uniqueServiceNames, WifiP2pDevice srcDevice) {
                if(uniqueServiceNames != null && srcDevice != null) {
                    debug_print("Found Service in device, :" + srcDevice.deviceName + ", with " + uniqueServiceNames.size() + " services");
                    for (int i = 0; i < uniqueServiceNames.size(); i++) {
                        String tmp = uniqueServiceNames.get(i);
                        debug_print("line(" + i + ") size = " + tmp.length() + " : " + tmp);
                    }
                } else {
                    debug_print("Service not there, uniqueServiceNames:" + uniqueServiceNames + ", srcDevice:" + srcDevice + ".");
                }

                //incase we are called after we have started the wait cycle
                //    if(myServiceState != ServiceState.WaitingTimer) {
                ServiceDiscoveryTimeOutTimer.cancel();
                peerDiscoveryTimer.cancel();
                peerDiscoveryTimer.start();
                //  }
            }
        };


        p2p.setUpnpServiceResponseListener(channel, serviceListener);
        startPeerDiscovery();
    }

    public void Stop() {
        this.context.unregisterReceiver(receiver);
        ServiceDiscoveryTimeOutTimer.cancel();
        peerDiscoveryTimer.cancel();
   //     PeerDiscoveryTimeOutTimer.cancel();
        stopDiscovery();
        stopPeerDiscovery();
    }

    private void startPeerDiscovery() {
    //    PeerDiscoveryTimeOutTimer.cancel();
    //    PeerDiscoveryTimeOutTimer.start();

        p2p.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
               // if(myServiceState != ServiceState.WaitingTimer) {
                    myServiceState = ServiceState.DiscoverPeer;
                    debug_print("Started peer discovery");
               // }else{
               //     debug_print("Keeping up visibility by starting peer discovery while waiting");
               // }
            }
            public void onFailure(int reason) {
                myServiceState = ServiceState.NONE;
                debug_print("Starting peer discovery failed, error code " + reason);
                if(callback != null) {
                    callback.PeerStartError(reason);
                }
                //lets try again after 1 minute time-out !
                ServiceDiscoveryTimeOutTimer.start();
            }
        });
    }

    private void stopPeerDiscovery() {
        p2p.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                debug_print("Stopped peer discovery");
            }

            public void onFailure(int reason) {
                debug_print("Stopping peer discovery failed, error code " + reason);
            }
        });
    }

    private void startServiceDiscovery() {

        myServiceState = ServiceState.DiscoverService;

        WifiP2pUpnpServiceRequest request = WifiP2pUpnpServiceRequest.newInstance();
        final Handler handler = new Handler();

        p2p.addServiceRequest(channel, request, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                debug_print("Added service request");
                handler.postDelayed(new Runnable() {
                    //There are supposedly a possible race-condition bug with the service discovery
                    // thus to avoid it, we are delaying the service discovery start here
                    public void run() {
                        p2p.discoverServices(channel, new WifiP2pManager.ActionListener() {
                            public void onSuccess() {
                                myServiceList.clear();
                                debug_print("Started service discovery");
                                myServiceState = ServiceState.DiscoverService;
                            }
                            public void onFailure(int reason) {
                                stopDiscovery();
                                myServiceState = ServiceState.NONE;
                                if(callback != null) {
                                    callback.ServiceStartError(reason);
                                }
                                debug_print("Starting service discovery failed, error code " + reason);
                                //lets try again after 1 minute time-out !
                                ServiceDiscoveryTimeOutTimer.start();
                            }
                        });
                    }
                }, 1000);
            }

            public void onFailure(int reason) {
                myServiceState = ServiceState.NONE;
                if(callback != null) {
                    callback.AddReqStartError(reason);
                }
                debug_print("Adding service request failed, error code " + reason);
                //lets try again after 1 minute time-out !
                ServiceDiscoveryTimeOutTimer.start();
            }
        });

    }

    private void stopDiscovery() {
        p2p.clearServiceRequests(channel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {debug_print("Cleared service requests");}
            public void onFailure(int reason) {debug_print("Clearing service requests failed, error code " + reason);}
        });
    }
    private void debug_print(String buffer) {
        Log.i("Service searcher", buffer);
    }
    private class ServiceSearcherReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                if(callback != null) {
                    callback.PeerChangedEvent();
                }
                if(myServiceState != ServiceState.DiscoverService) {
                //&& myServiceState != ServiceState.WaitingTimer) {
                    p2p.requestPeers(channel, peerListListener);
                }
            } else if(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action)) {
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED);
                String persTatu = "Discovery state changed to ";

                if (state == WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED) {
                    if (callback != null) {
                        callback.PeerDiscoveryStopped();
                    }
                    persTatu = persTatu + "Stopped.";
                    startPeerDiscovery();
                } else if (state == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED) {
                    persTatu = persTatu + "Started.";
                } else {
                    persTatu = persTatu + "unknown  " + state;
                }
                debug_print(persTatu);

            }
        }
    }
}
