package xml_mike.radioalarm.managers;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import xml_mike.radioalarm.models.RadioFactory;
import xml_mike.radioalarm.models.RadioStation;

/**
 * Created by MClifford on 13/05/15.
 *
 * all streams located in database interface, gets all database objects and adds them to a list.
 */
public class RadioStationsManager {
    private static final String baseURl = "http://api.dirble.com/v2/countries/"; //params[0]
    private static final String country = "GB/"; //params[1]
    private static final String type = "stations/"; //params[2]
    private static final String per_page = "30";  //params[3]
    private static final String token = "&token=b3b1e7e015ac9cb7104006f1e0"; //params[4]
    private static RadioStationsManager ourInstance;
    private ArrayList<RadioStation> radioStations; //potentially convert to hash map for quicker retrieval
    private RadioStationsManager() {
        radioStations = DatabaseManager.getInstance().getAllRadioStations();
    }

    public static RadioStationsManager getInstance() {
        if(ourInstance == null)
            ourInstance = new RadioStationsManager();

        return ourInstance;
    }

    public static RadioStation retrieveRadioStation(long ID){
        return DatabaseManager.getInstance().getRadioStation(ID);
    }

    public ArrayList<RadioStation> getRadioStations() {
        return radioStations;
    }

    public void setRadioStations(ArrayList<RadioStation> radioStations) {
        this.radioStations = radioStations;
    }

    public void downloadStations(){
        new StationDownloader().execute(baseURl, country, type, per_page, token);
    }

    public void addRadioStation(RadioStation radioStation){
        radioStations.add(radioStation);
        DatabaseManager.getInstance().addRadioStation(radioStation);
    }

    public RadioStation getRadioStation(int position){
        return radioStations.get(position);
    }

    public RadioStation searchRadioStation(Long ID){

        for(RadioStation radioStation: radioStations) {
            if(radioStation.getId() == ID){
                return radioStation;
            }
        }

        return new RadioStation();
    }

    public void setRadioStation(int i,RadioStation radioStation){
        radioStations.set(i, radioStation);
    }

    private class StationDownloader extends AsyncTask<String , String, String> {

        @Override
        protected String doInBackground(String... params) {

            //initialise all variables needed for download
            InputStream inputStream = null;
            HttpURLConnection connection = null;
            int page=1;
            boolean responseEmpty = false;
            try {
               do{
                    inputStream = null;
                    connection = null;
                    URL url = new URL(baseURl+country+type+"?per_page="+per_page+"&page="+page+token); //Complied URL from static Strings
                    connection = (HttpURLConnection) url.openConnection();
                    connection.connect();
                    Log.e("Begin", "(" + page+"){" );
                    // expect HTTP 200 OK, so we don't mistakenly save error report
                    // instead of the file
                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        break;
                    }

                    // this will be useful to display download percentage
                    // might be -1: server did not report the length
                    int fileLength = connection.getContentLength(); //may be used later

                    // download the file
                    inputStream = connection.getInputStream();

                    BufferedReader reader = new BufferedReader(new InputStreamReader( inputStream, "utf-8"), 8);
                    StringBuilder sb = new StringBuilder();

                    for(String line = reader.readLine(); line != null; line = reader.readLine()){
                        sb.append(line);
                    }

                    JSONArray returnArray = new JSONArray( sb.toString());

                    if(returnArray.length() > 0) {
                        for (int i = 0; i < returnArray.length(); i++) {
                            RadioStation rad = RadioFactory.generateRadionStation(returnArray.getJSONObject(i));
                            if(rad.getStreams().size() >0)
                                addRadioStation(rad) ;
                        }
                    }

                   if(returnArray.length() < Integer.parseInt(per_page))
                        responseEmpty = true;

                    Log.e("End","}("+page+")");
                    page++;
                } while(!responseEmpty);

            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (inputStream != null)
                        inputStream.close();
                } catch (IOException ignored) {
                    Log.e("DOM", ignored.toString());
                }

                if (connection != null)
                    connection.disconnect();
            }
            return "Complete";
        }
    }

}