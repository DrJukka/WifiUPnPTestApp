package org.thaliproject.p2p.wifiupnptest;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by juksilve on 6.3.2015.
 */
public class SearchService extends Service implements WifiBase.WifiStatusCallBack {

    SearchService that = this;

    static final public String DSS_WIFIDIRECT_VALUES = "test.microsoft.com.wifidirecttest.DSS_WIFIDIRECT_VALUES";
    static final public String DSS_WIFIDIRECT_MESSAGE = "test.microsoft.com.wifidirecttest.DSS_WIFIDIRECT_MESSAGE";


    CountDownTimer ServiceFoundTimeOutTimer = new CountDownTimer(600000, 1000) {
        public void onTick(long millisUntilFinished) {
            // not using
        }
        public void onFinish() {
            ResetCounterCount = ResetCounterCount + 1;
            //Restart service discovery
            startServices();
        }
    };


    CountDownTimer WifiResetTimeOutTimer = new CountDownTimer(1800000, 1000) { //
        public void onTick(long millisUntilFinished) {
            // not using
        }
        public void onFinish() {
            WifiCounterCount = WifiCounterCount + 1;
            //Restart service discovery
            //startServices();

            //switch off Wlan :)
            if(mWifiBase != null) {
                ITurnedWifiOff = true;
                mWifiBase.setWifiEnabled(false);
            }
        }
    };

    // 20 minute timer
    CountDownTimer SaveDataTimeOutTimer = new CountDownTimer(1200000, 1000) {
        public void onTick(long millisUntilFinished) {
            // not using
        }
        public void onFinish() {
            if (mTestDataFile != null) {
                //long Started , long got , long Noservices ,long Peererr ,long ServiceErr , long AddreqErr ,long  resetcounter) {
                mTestDataFile.WriteDebugline(lastChargePercent,peersFoundCount,fullRoundCount,noServicesCount,PeerErrorCount,ServErrorCount,AddRErrorCount,ResetCounterCount,PeerChangedEventCount,PeerDiscoveryStoppedCount,WaitTimeExpiredCount,WifiCounterCount);
            }
            SaveDataTimeOutTimer.start();
        }
    };

    Boolean ITurnedWifiOff = false;

    long WifiCounterCount = 0;
    long PeerErrorCount = 0;
    long ServErrorCount = 0;
    long AddRErrorCount = 0;
    long ResetCounterCount = 0;
    long peersFoundCount = 0;
    long fullRoundCount = 0;
    long noServicesCount = 0;
    long PeerChangedEventCount = 0;
    long PeerDiscoveryStoppedCount = 0;
    long WaitTimeExpiredCount = 0;

    String latsDbgString = "";
    WifiBase mWifiBase = null;
    WifiServiceAdvertiser mWifiAccessPoint = null;
    WifiServiceSearcher mWifiServiceSearcher = null;

    IntentFilter mfilter = null;
    BroadcastReceiver mReceiver = null;
    TestDataFile mTestDataFile = null;
    int lastChargePercent = -1;

    private final IBinder mBinder = new MyLocalBinder();

    @Override
    public void WifiStateChanged(int state) {
        if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
            // we got wifi back, so we can re-start now
            print_line("WB", "Wifi is now enabled !");
            startServices();
        } else {
            //no wifi availavble, thus we need to stop doing anything;
            print_line("WB", "Wifi is DISABLEd !!");
            stopServices();

            if(mWifiBase != null && ITurnedWifiOff) {
                WifiResetTimeOutTimer.start();
                ITurnedWifiOff = false;
                mWifiBase.setWifiEnabled(true);
            }
        }
    }

    @Override
    public void gotPeersList(Collection<WifiP2pDevice> list) {
        peersFoundCount = peersFoundCount + 1;
        ServiceFoundTimeOutTimer.cancel();
        ServiceFoundTimeOutTimer.start();
    }

    @Override
    public void gotServicesList(List<ServiceItem> list) {
        if(mWifiServiceSearcher != null & mWifiBase != null) {
            // Select service, save it in a list and start connection with it
            // and do remember to cancel Searching
            if (list != null && list.size() > 0) {
                print_line("SS", "Services found: " + list.size());

                fullRoundCount = fullRoundCount + 1;

                ServiceItem selItem = mWifiBase.SelectServiceToConnect(list);
                if (selItem != null) {
                    String message = "Round " + fullRoundCount + ", we found " + list.size()+ " servces and selected " + selItem.deviceName;

                    Intent intent = new Intent(DSS_WIFIDIRECT_VALUES);
                    intent.putExtra(DSS_WIFIDIRECT_MESSAGE, message);
                    sendBroadcast(intent);

                    // testing how this would reduce the battery consumption

                    ServiceFoundTimeOutTimer.cancel();
                    ServiceFoundTimeOutTimer.start();

                    print_line("", "Sent broadcast : " + message);
                } else {
                    // we'll get discovery stopped event soon enough
                    // and it starts the discovery again, so no worries :)
                    print_line("", "No devices selected");
                }
            }else{
                noServicesCount = noServicesCount + 1;
                print_line("", "No services found ?? from: " + list.size() + " service");
            }
        }
    }

    @Override
    public void gotDnsTXTRecordList(Map<String, String> mapping) {

        String message = "Got Txtx with: "  + mapping.size() + " item. ";

        if(mapping.containsKey("0")){
            message = message + "\n0 contains " + mapping.get("0").length() + " bytes, ";
        }

        if(mapping.containsKey("1")){
            message = message + "\n1 contains " + mapping.get("1").length() + " bytes, ";
        }

        if(mapping.containsKey("2")){
            message = message + "\n2 contains " + mapping.get("2").length() + " bytes, ";
        }

        if(mapping.containsKey("3")){
            message = message + "\n3 contains " + mapping.get("3").length() + " bytes, ";
        }

        if(mapping.containsKey("4")){
            message = message + "\n4 contains " + mapping.get("4").length() + " bytes, ";
        }

        if(mapping.containsKey("5")){
            message = message + "\n5 contains " + mapping.get("5").length() + " bytes, ";
        }

        if(mapping.containsKey("6")){
            message = message + "\n6 contains " + mapping.get("6").length() + " bytes, ";
        }

        Intent intent = new Intent(DSS_WIFIDIRECT_VALUES);
        intent.putExtra(DSS_WIFIDIRECT_MESSAGE, message);
        sendBroadcast(intent);
    }

    @Override
    public void PeerStartError(int error) {
        PeerErrorCount = PeerErrorCount + 1;
    }

    @Override
    public void ServiceStartError(int error) {
        ServErrorCount = ServErrorCount + 1;
    }

    @Override
    public void AddReqStartError(int error) {
        AddRErrorCount = AddRErrorCount + 1;
    }

    @Override
    public void PeerChangedEvent() {PeerChangedEventCount = PeerChangedEventCount + 1;

    }

    @Override
    public void PeerDiscoveryStopped() {PeerDiscoveryStoppedCount = PeerDiscoveryStoppedCount + 1;

    }

    @Override
    public void WaitTimeCallback() {WaitTimeExpiredCount = WaitTimeExpiredCount + 1;}

    public class MyLocalBinder extends Binder {
        SearchService getService() {
            return SearchService.this;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        print_line("SearchService","onStartCommand rounds so far :" + fullRoundCount);
        Start();
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        print_line("SearchService","onDestroy");
        super.onDestroy();
        ServiceFoundTimeOutTimer.cancel();
        Stop();
    }

    public void Start() {

      //  mTestDataFile = new TestDataFile(this);
        SaveDataTimeOutTimer.start();

        mfilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        mReceiver = new PowerConnectionReceiver();
        registerReceiver(mReceiver, mfilter);

        WifiResetTimeOutTimer.start();
        startServices();
    }

    public void Stop() {
        WifiResetTimeOutTimer.cancel();

        if(mReceiver != null){
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }

        stopServices();
        if(mWifiBase != null){
            mWifiBase.Stop();
            mWifiBase = null;
        }

        if(mTestDataFile != null) {
            print_line("SearchService","Closing File");
            mTestDataFile.CloseFile();
            mTestDataFile = null;
        }

        SaveDataTimeOutTimer.cancel();
    }

    long roundsCount(){
        return fullRoundCount;
    }

    String getLastDbgString() {

        String ret = "peers : " + peersFoundCount + ", fullrounds: " + fullRoundCount + "\n";
        ret =  ret + "Reset counter: " + ResetCounterCount + ", last charge: " + lastChargePercent + "%\n";
        ret =  ret + "PErr: " + PeerErrorCount + ", SErr: " + ServErrorCount + ", AddErr: " + AddRErrorCount + ", No service counter: " + noServicesCount + "\n";
        ret =  ret + "last: " + latsDbgString + "\n";

        return ret;
    }

    boolean isRunnuing(){
        boolean ret = false;
        if(mWifiBase != null){
            ret = true;
        }
        return ret;
    }

    private void stopServices(){
        print_line("","Stoppingservices");
        if(mWifiAccessPoint != null){
            mWifiAccessPoint.Stop();
            mWifiAccessPoint = null;
        }

        if(mWifiServiceSearcher != null){
            mWifiServiceSearcher.Stop();
            mWifiServiceSearcher = null;
        }
        print_line("","Services stopped");
    }

    private void startServices(){

        stopServices();

        if(mWifiBase == null){
            mWifiBase = new WifiBase(this,this);
            mWifiBase.Start();
        }

        WifiP2pManager.Channel channel = mWifiBase.GetWifiChannel();
        WifiP2pManager p2p = mWifiBase.GetWifiP2pManager();

        if(channel != null && p2p != null) {
            print_line("", "Starting services");
            mWifiAccessPoint = new WifiServiceAdvertiser(p2p, channel);

           // String test = getStringWithLengthAndFilledWithCharacter(99,'h');
           // mWifiAccessPoint.Start(test, WifiBase.SERVICE_TYPE);

            mWifiAccessPoint.Start("powerTests", WifiBase.SERVICE_TYPE);

            mWifiServiceSearcher = new WifiServiceSearcher(this, p2p, channel, that, WifiBase.SERVICE_TYPE);
            mWifiServiceSearcher.Start();
            print_line("", "services started");
        }
    }

    protected String getStringWithLengthAndFilledWithCharacter(int length, char charToFill) {
        char[] array = new char[length];
        int pos = 0;
        while (pos < length) {
            array[pos] = charToFill;
            pos++;
        }
        return new String(array);
    }



    public void print_line(String who, String line) {
        latsDbgString = who + " : " + line;
        Log.i("BtTestMaa" + who, line);
    }

    public class PowerConnectionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

       //     print_line("", "Action : " + intent.getAction());

            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);

            lastChargePercent = (level*100)/scale;

            String message = "Battery charge: " + lastChargePercent + " %";
        }
    }
}



