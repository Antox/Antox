package im.tox.antox.utils;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import im.tox.antox.tox.ToxDoService;

public class DHTNodeDetails extends AsyncTask<Void, Void, Void> {

    final String[] nodeDetails = new String[7];
    final String TAG = "DHTNODEDETAILS";
    Context ctx;

    public DHTNodeDetails(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            // Connect to the web site
            Document document = Jsoup.connect("https://wiki.tox.im/Nodes").timeout(10000).get();
            Elements nodeRows = document.getElementsByTag("tr");

            File folder = new File(Environment.getExternalStorageDirectory() + "/Antox");
            final String filename = folder.toString() + "/" + "Nodes.csv";
            FileWriter fw = new FileWriter(filename);

            for (Element nodeRow : nodeRows) {
                Elements nodeElements = nodeRow.getElementsByTag("td");
                int c = 0;
                for (Element nodeElement : nodeElements)
                    nodeDetails[c++] = nodeElement.text();

                if (nodeDetails[6] != null && nodeDetails[6].equals("WORK")) {
                    DhtNode.ipv4.add(nodeDetails[0]);
                    DhtNode.ipv6.add(nodeDetails[1]);
                    DhtNode.port.add(nodeDetails[2]);
                    DhtNode.key.add(nodeDetails[3]);
                    DhtNode.owner.add(nodeDetails[4]);
                    DhtNode.location.add(nodeDetails[5]);
                    fw.append(nodeDetails[0]+","+nodeDetails[1]+","+nodeDetails[2]+","+nodeDetails[3]
                            +","+nodeDetails[4]+","+nodeDetails[5]+"\n");
                }
            }

            fw.close();

            Log.d(TAG, "Nodes fetched from online");

        } catch (Exception e) {

            String fileName = "AntoxNodes";
            BufferedReader br;
            String line;
            String splitBy = ",";

            try {
                br = new BufferedReader(new FileReader(fileName));
                String[] node;
                while((line = br.readLine()) != null) {
                    node = line.split(splitBy);
                    DhtNode.ipv4.add(node[0]);
                    DhtNode.ipv6.add(node[1]);
                    DhtNode.location.add(node[2]);
                    DhtNode.owner.add(node[3]);
                    DhtNode.port.add(node[4]);
                    DhtNode.key.add(node[5]);
                }
                Log.d(TAG, "AntoxNodes file found and been read");
            } catch (Exception exp) {
                Log.d(TAG, "AntoxNodes file not found");
                DhtNode.ipv4.add("192.254.75.98");
                DhtNode.ipv6.add("2607:5600:284::2");
                DhtNode.location.add("US");
                DhtNode.owner.add("stqism");
                DhtNode.port.add("33445");
                DhtNode.key.add("951C88B7E75C867418ACDB5D273821372BB5BD652740BCDF623A4FA293E75D2F");

                DhtNode.ipv4.add("144.76.60.215");
                DhtNode.ipv6.add("2a01:4f8:191:64d6::1");
                DhtNode.location.add("DE");
                DhtNode.owner.add("sonofra");
                DhtNode.port.add("33445");
                DhtNode.key.add("04119E835DF3E78BACF0F84235B300546AF8B936F035185E2A8E9E0A67C8924F");

                DhtNode.ipv4.add("37.187.46.132");
                DhtNode.ipv6.add("2001:41d0:0052:0300::0507");
                DhtNode.location.add("FR");
                DhtNode.owner.add("mouseym");
                DhtNode.port.add("33445");
                DhtNode.key.add("A9D98212B3F972BD11DA52BEB0658C326FCCC1BFD49F347F9C2D3D8B61E1B927");

                DhtNode.ipv4.add("109.169.46.133");
                DhtNode.ipv6.add("");
                DhtNode.location.add("UK");
                DhtNode.owner.add("astonex");
                DhtNode.port.add("33445");
                DhtNode.key.add("7F31BFC93B8E4016A902144D0B110C3EA97CB7D43F1C4D21BCAE998A7C838821");

                DhtNode.ipv4.add("54.199.139.199");
                DhtNode.ipv6.add("");
                DhtNode.location.add("JP");
                DhtNode.owner.add("aitjcize");
                DhtNode.port.add("33445");
                DhtNode.key.add("7F9C31FE850E97CEFD4C4591DF93FC757C7C12549DDD55F8EEAECC34FE76C029");
            }
        }

        Log.d(TAG, "DhtNode size: " + DhtNode.ipv4.size());

        int times[][] = new int[DhtNode.ipv4.size()][1];

        // Initialise array
        for(int i = 0; i < DhtNode.ipv4.size(); i++)
            times[i][0] = 500;

        final ExecutorService service;
        // Create a thread pool equal to the amount of nodes
        service = Executors.newFixedThreadPool(DhtNode.ipv4.size());
        List<Future<int[]>> list = new ArrayList<Future<int[]>>();
        for(int i = 0; i < DhtNode.ipv4.size(); i++) {
            Callable<int[]> worker = new PingServers(i);
            Future<int[]> submit = service.submit(worker);
            list.add(submit);
        }

        // Get all the times back from the threads
        for(Future<int[]> future : list) {
            try {
                int tmp[] = future.get();
                times[tmp[0]][0] = tmp[1];
            } catch(ExecutionException e) {
                e.printStackTrace();
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Find shortest time
        int shortest = 500;
        int pos = -1;
        for(int i = 0; i < DhtNode.ipv4.size(); i++) {
            if(times[i][0] < shortest) {
                shortest = times[i][0];
                pos = i;
            }
        }

        // Move shortest node to front
        if(pos != -1) {
            DhtNode.ipv4.add(0, DhtNode.ipv4.get(pos));
            DhtNode.ipv6.add(0, DhtNode.ipv6.get(pos));
            DhtNode.port.add(0, DhtNode.port.get(pos));
            DhtNode.key.add(0, DhtNode.key.get(pos));
            DhtNode.owner.add(0, DhtNode.owner.get(pos));
            DhtNode.location.add(0, DhtNode.location.get(pos));
            Log.d(TAG, "DHT Nodes have been sorted");
            DhtNode.sorted = true;
        }

        return null;
    }

    private class PingServers implements Callable<int[]> {
        final int number;

        public PingServers(int i) {
            this.number = i;
        }

        public int[] call() {
            int result[] = { number, 500 };
            try {
                long currentTime = System.currentTimeMillis();
                boolean reachable = InetAddress.getByName(DhtNode.ipv4.get(number)).isReachable(400);
                long elapsedTime = System.currentTimeMillis() - currentTime;
                if(reachable)
                    result[1] = (int)elapsedTime;
            } catch (IOException e) {
                e.printStackTrace();
                e.printStackTrace();
            }

            return result;
        }
    }

    @Override
    protected void onPostExecute(Void result)
    {
        /**
         * There is a chance that downloading finishes later than the bootstrapping call in the
         * ToxService, because both are in separate threads. In that case to make sure the nodes
         * are bootstrapped we restart the ToxService
         */
        if(!DhtNode.connected)
        {
            Log.d(TAG, "Restarting START_TOX as DhtNode.connected returned false");
            Intent restart = new Intent(ctx, ToxDoService.class);
            restart.setAction(Constants.START_TOX);
            ctx.startService(restart);
        }

            /* Restart intent if it was connected before nodes were sorted */
        if(DhtNode.connected && !DhtNode.sorted) {
            Log.d(TAG, "Restarting START_TOX as DhtNode.sorted was false");
            Intent restart = new Intent(ctx, ToxDoService.class);
            restart.setAction(Constants.START_TOX);
            ctx.startService(restart);
        }
    }
}
