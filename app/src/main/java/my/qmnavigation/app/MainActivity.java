package my.qmnavigation.app;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

public class MainActivity extends Activity implements LocationListener {
    private LocationManager manager;
    private TextToSpeech speech;
    private EditText latitude, longitude;
    private TextView status;
    private Location current;
    private double targetLat, targetLon;
    private boolean navigating = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(20 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);
        TextView title = new TextView(this);
        title.setText("NAVIGASI NYAMUK — GPS ke koordinat destinasi");
        title.setTextSize(21);
        layout.addView(title);
        latitude = new EditText(this);
        latitude.setHint("Latitud destinasi (cth: 6.125)");
        latitude.setInputType(8194);
        layout.addView(latitude);
        longitude = new EditText(this);
        longitude.setHint("Longitud destinasi (cth: 102.238)");
        longitude.setInputType(8194);
        layout.addView(longitude);
        Button start = new Button(this);
        start.setText("Mula navigasi GPS");
        layout.addView(start);
        Button stop = new Button(this);
        stop.setText("Henti navigasi");
        layout.addView(stop);
        status = new TextView(this);
        status.setTextSize(18);
        status.setText("Menunggu kebenaran lokasi.");
        layout.addView(status);
        setContentView(layout);
        manager = (LocationManager)getSystemService(LOCATION_SERVICE);
        speech = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) speech.setLanguage(new Locale("ms", "MY"));
        });
        start.setOnClickListener(v -> {
            try {
                double lat = Double.parseDouble(latitude.getText().toString().trim());
                double lon = Double.parseDouble(longitude.getText().toString().trim());
                if (!Double.isFinite(lat) || !Double.isFinite(lon) ||
                    lat < -90 || lat > 90 || lon < -180 || lon > 180) throw new NumberFormatException();
                targetLat = lat; targetLon = lon; navigating = true;
                status.setText("Mencari lokasi GPS...");
                enableLocation();
                if (current != null) updateNavigation(current);
            } catch (NumberFormatException e) {
                status.setText("Koordinat tidak sah. Masukkan latitud -90 hingga 90 dan longitud -180 hingga 180.");
            }
        });
        stop.setOnClickListener(v -> {
            navigating = false;
            manager.removeUpdates(this);
            if (speech != null) speech.stop();
            status.setText("Navigasi dihentikan.");
        });
        enableLocation();
    }

    private void enableLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }
        try {
            if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                status.setText("Hidupkan GPS pada tetapan telefon.");
                return;
            }
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 2, this);
        } catch (SecurityException e) {
            status.setText("Kebenaran lokasi diperlukan.");
        }
    }

    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code == 100 && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) enableLocation();
        else status.setText("Kebenaran GPS tidak diberikan.");
    }

    @Override public void onLocationChanged(Location location) {
        current = location;
        if (navigating) updateNavigation(location);
    }

    private void updateNavigation(Location location) {
        Location destination = new Location("destination");
        destination.setLatitude(targetLat);
        destination.setLongitude(targetLon);
        float distance = location.distanceTo(destination);
        float bearing = location.bearingTo(destination);
        String message = String.format(Locale.getDefault(),
            "Jarak terus: %.0f meter\nArah destinasi: %.0f° dari utara\nKetepatan GPS: %.0f meter\n\nIni panduan arah terus, BUKAN laluan jalan raya.",
            distance, (bearing + 360) % 360, location.getAccuracy());
        status.setText(message);
        if (distance < 25 && location.hasAccuracy() && location.getAccuracy() < 30) {
            if (speech != null) speech.speak("Anda berhampiran destinasi", TextToSpeech.QUEUE_FLUSH, null, "arrival");
            navigating = false;
        }
    }

    @Override protected void onDestroy() {
        manager.removeUpdates(this);
        if (speech != null) { speech.stop(); speech.shutdown(); }
        super.onDestroy();
    }
}
