// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.
package org.thaliproject.p2p.wifiupnptest;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.text.format.Time;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Created by juksilve on 6.3.2015.
 */
public class TestDataFile {

    private final String fileNameStart = "PowWithDelay";
    private final String firstLine= "Os ,time ,battery ,Started ,got ,No services ,Peer err ,Service Err ,Add req Err ,reset counter,Peers changed, discovery stopped, wait expired, wifi reset \n";


    private File dbgFile;
    private OutputStream dbgFileOs;
    private Context context;

    public TestDataFile(Context Context){
        this.context = Context;
        Time t= new Time();
        t.setToNow();

        File path = this.context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);

        String sFileName =  "/" + fileNameStart + t.yearDay + t.hour+ t.minute + t.second + ".txt";

        try {
            dbgFile = new File(path, sFileName);
            dbgFileOs = new FileOutputStream(dbgFile);
            dbgFileOs.write(firstLine.getBytes());
            dbgFileOs.flush();

            Log.d(fileNameStart, "File created:" + path + " ,filename : " + sFileName);
        }catch(Exception e){
            Log.d("FILE", "FileWriter, create file error, :" + e.toString());
        }
    }

    public void CloseFile(){
        try {
            if (dbgFile != null) {
                dbgFileOs.close();
                dbgFile.delete();
            }
        }catch (Exception e){
            Log.d(fileNameStart, "dbgFile close error :" + e.toString());
        }
    }

    public void WriteDebugline(int battery, long Started , long got , long Noservices ,long Peererr ,long ServiceErr , long AddreqErr ,long  resetcounter,long  PeerChangedEventCount,long  PeerDiscoveryStoppedCount, long WaitTimeExpiredCount, long WifiCounterCount) {

        //"Os ,time ,battery ,Started ,got ,No services ,Peer err ,Service Err ,Add req Err ,reset counter \n";

        try {
            String dbgData = Build.VERSION.SDK_INT + " ," ;
            dbgData = dbgData  + System.currentTimeMillis() + " ,";
            dbgData = dbgData + battery + " ,";
            dbgData = dbgData + Started + " ,";
            dbgData = dbgData + got + " ,";
            dbgData = dbgData + Noservices + " ,";
            dbgData = dbgData + Peererr + " ,";
            dbgData = dbgData + ServiceErr + " ,";
            dbgData = dbgData + AddreqErr + " ,";
            dbgData = dbgData + resetcounter + " ,";
            dbgData = dbgData + PeerChangedEventCount + " ,";
            dbgData = dbgData + PeerDiscoveryStoppedCount + " ,";
            dbgData = dbgData + WaitTimeExpiredCount + " ,";
            dbgData = dbgData + WifiCounterCount + " \n";

            Log.d("FILE", "write: " + dbgData);
            dbgFileOs.write(dbgData.getBytes());
            dbgFileOs.flush();

        }catch(Exception e){
            Log.d("FILE", "dbgFile write error :" + e.toString());
        }
    }
}
