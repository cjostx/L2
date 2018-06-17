package pird.com.l2;

import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private TextView mTextMessage;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    mTextMessage.setText(R.string.title_home);
                    return true;
                case R.id.navigation_dashboard:
                    mTextMessage.setText(R.string.title_dashboard);
                    return true;
                case R.id.navigation_notifications:
                    mTextMessage.setText(R.string.title_notifications);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy =
                    new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        setContentView(R.layout.activity_main);

        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        OkHttpClient client = new OkHttpClient();

        HttpUrl sURL = new HttpUrl.Builder()
                .scheme("http")
                .host("pird.ddns.net")
                .port(8086)
                .addPathSegment("query")
                .addQueryParameter("pretty", "true")
                .addQueryParameter("db", "pird")
                .addEncodedQueryParameter("q", "SELECT value FROM active_power WHERE device = 'PIRD-SMARTMETER-1' AND time >= now() - 1h")
                .build();
        Request request = new Request.Builder().url(sURL).build();
        JSONObject resQuery = null;
        JSONArray rQuery = null;
        try (Response response = client.newCall(request).execute()) {
            resQuery = new JSONObject(response.body().string());
            resQuery = (JSONObject) resQuery.getJSONArray("results").get(0);
            resQuery = (JSONObject) resQuery.getJSONArray("series").get(0);
            rQuery = resQuery.getJSONArray("values");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        /*for (int i = 0; i < rQuery.length(); i++) {
            try {
                JSONArray values = rQuery.getJSONArray(i);
                System.out.println(values.get(0) +"-"+ values.get(1));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }*/


        GraphView graph = (GraphView) findViewById(R.id.graph);
        DataPoint[] sPoint = new DataPoint[rQuery.length()];
        for (int i = 0; i < rQuery.length(); i++) {
            try {
                JSONArray values = rQuery.getJSONArray(i);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date dt = sdf.parse(values.get(0).toString());
                sPoint[i] = new DataPoint(dt, (double) values.getDouble(1));
                System.out.println(dt.getTime());
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(sPoint);


        graph.addSeries(series);

        graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(this.getApplication(), new SimpleDateFormat("HH:mm:ss")));
        graph.getGridLabelRenderer().setNumHorizontalLabels(3); // only 4 because of the space

// set manual x bounds to have nice steps
        graph.getViewport().setMinY(0);
        graph.getViewport().setYAxisBoundsManual(true);

// as we use dates as labels, the human rounding to nice readable numbers
// is not necessary
        graph.getGridLabelRenderer().setHumanRounding(true);

    }

}
