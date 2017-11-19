package io.nlopez.smartlocation.sample;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.utils.Nulls;

public class GeofencingFragment extends Fragment {
    private static final Pattern LATITUDE_REGEX =
            Pattern.compile("^(\\+|-)?(?:90(?:(?:\\.0{1,6})?)|(?:[0-9]|[1-8][0-9])(?:(?:\\.[0-9]{1,6})?))$");

    private static final Pattern LONGITUDE_REGEX =
            Pattern.compile("^(\\+|-)?(?:180(?:(?:\\.0{1,6})?)|(?:[0-9]|[1-9][0-9]|1[0-7][0-9])(?:(?:\\.[0-9]{1,6})?))$");

    private EditText mLatitudeText;
    private EditText mLongitudeText;
    private Button mAddGeofenceButton;
    private RadioButton mEnterRadioButton;
    private RadioButton mExitRadioButton;
    private RadioButton mDwellRadioButton;
    private PendingIntent mGeofencePendingIntent;

    private final View.OnClickListener mAddClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Convert text input to location if possible
            final String latitudeText = mLatitudeText.getText().toString();
            final Double latitude = Double.parseDouble(latitudeText);
            final String longitudeText = mLongitudeText.getText().toString();
            final Double longitude = Double.parseDouble(longitudeText);
            final Location location = new Location("test");
            location.setLatitude(latitude);
            location.setLongitude(longitude);

            // Prepare the geofence
            final int transition;
            if (mEnterRadioButton.isChecked()) {
                transition = Geofence.GEOFENCE_TRANSITION_ENTER;
            } else if (mExitRadioButton.isChecked()) {
                transition = Geofence.GEOFENCE_TRANSITION_EXIT;
            } else {
                transition = Geofence.GEOFENCE_TRANSITION_DWELL;
            }
            final List<Geofence> geofenceList = new ArrayList<>();
            geofenceList.add(new Geofence.Builder()
                    .setRequestId("test")
                    .setCircularRegion(latitude, longitude, 100)
                    .setLoiteringDelay(30000) // half a minute
                    .setTransitionTypes(transition)
                    .setExpirationDuration(600) // 10 minutes
                    .build());

            // Prepare the request
            final GeofencingRequest request = new GeofencingRequest.Builder()
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                    .addGeofences(geofenceList)
                    .build();

            // Add the geofence
            SmartLocation.with(getContext())
                    .geofencing()
                    .addGeofences(request, getGeofencePendingIntent());
            snackText("Geofence added (valid for 10 minutes, 100 meters diameter, " + getGeofenceTransitionString(transition) + ")");
        }
    };

    private final TextWatcher mLatLngTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            final String latitudeText = mLatitudeText.getText().toString();
            final String longitudeText = mLongitudeText.getText().toString();

            final boolean hasLatitude = !TextUtils.isEmpty(latitudeText)
                    && LATITUDE_REGEX.matcher(latitudeText).matches();
            final boolean hasLongitude = !TextUtils.isEmpty(longitudeText)
                    && LONGITUDE_REGEX.matcher(longitudeText).matches();

            mAddGeofenceButton.setEnabled(hasLatitude && hasLongitude);
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_geofencing, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mAddGeofenceButton = view.findViewById(R.id.add_geofence_button);
        mAddGeofenceButton.setOnClickListener(mAddClickListener);
        mLatitudeText = view.findViewById(R.id.latitude);
        mLongitudeText = view.findViewById(R.id.longitude);
        mLatitudeText.addTextChangedListener(mLatLngTextWatcher);
        mLongitudeText.addTextChangedListener(mLatLngTextWatcher);
        mEnterRadioButton = view.findViewById(R.id.radio_enter);
        mExitRadioButton = view.findViewById(R.id.radio_exit);
        mDwellRadioButton = view.findViewById(R.id.radio_dwell);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mLatitudeText.removeTextChangedListener(mLatLngTextWatcher);
        mLongitudeText.removeTextChangedListener(mLatLngTextWatcher);
    }

    @NonNull
    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(getContext(), GeofenceIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        mGeofencePendingIntent = PendingIntent.getService(getContext(),
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }

    private void snackText(@NonNull String message) {
        Snackbar.make(Nulls.notNull(getView()), message, Snackbar.LENGTH_LONG).show();
    }

    @NotNull
    private static String getGeofenceTransitionString(int transition) {
        switch (transition) {
            case Geofence.GEOFENCE_TRANSITION_DWELL:
                return "dwell";
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return "enter";
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return "exit";
            default:
                return "¯\\_(ツ)_/¯";
        }
    }

    public static class GeofenceIntentService extends IntentService {
        private static final String TAG = GeofenceIntentService.class.getSimpleName();
        private static final int NOTIFICATION_ID = 1234;

        public GeofenceIntentService() {
            super("geofence_svc");
        }

        @Override
        protected void onHandleIntent(@Nullable Intent intent) {
            final GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
            if (geofencingEvent == null || geofencingEvent.hasError()) {
                Log.e(TAG, "Error in geofencing event " + geofencingEvent);
                return;
            }

            // Get the transition type.
            final int geofenceTransition = geofencingEvent.getGeofenceTransition();

            // Create a notification to display the geofence intent
            final NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.ic_geofence_notification)
                            .setContentTitle("SmartLocation Geofence Alert");

            final StringBuilder builder = new StringBuilder();
            builder.append("[");
            builder.append(getGeofenceTransitionString(geofenceTransition));
            builder.append("]");

            // Get the geofences that were triggered. A single event can trigger
            // multiple geofences.
            for (final Geofence geofence : geofencingEvent.getTriggeringGeofences()) {
                builder.append(" ");
                builder.append(geofence.getRequestId());
            }
            notificationBuilder.setContentText(builder);

            // Gets an instance of the NotificationManager service
            final NotificationManager notificationManager =
                    Nulls.notNull((NotificationManager) getSystemService(NOTIFICATION_SERVICE));
            // Builds the notification and issues it.
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
        }
    }
}
