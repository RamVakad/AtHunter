package com.example.athunter.util;

import com.example.athunter.global.config.AppConfig;
import com.example.athunter.model.Tweet;
import com.firebase.ui.database.SnapshotParser;
import com.google.firebase.database.DataSnapshot;

public class GeneralTools {

    public static boolean isUserInRange(double latitude, double longitude) {
        double meterDistance = distance(latitude, AppConfig.HUNTER_LATITUDE, longitude, AppConfig.HUNTER_LONGITUDE, 0, 0);
        return meterDistance < AppConfig.MAX_DISTANCE;
    }

    public static SnapshotParser<Tweet> getTweetParser() {
        return new SnapshotParser<Tweet>() {
            @Override
            public Tweet parseSnapshot(DataSnapshot dataSnapshot) {
                Tweet tweet = dataSnapshot.getValue(Tweet.class);
                if (tweet != null) {
                    tweet.setId(dataSnapshot.getKey());
                }
                return tweet;
            }
        };
    }

    /**
     * Calculate distance between two points in latitude and longitude taking
     * into account height difference. If you are not interested in height
     * difference pass 0.0. Uses Haversine method as its base.
     *
     * lat1, lon1 Start point lat2, lon2 End point el1 Start altitude in meters
     * el2 End altitude in meters
     * @returns Distance in Meters
     */
    public static double distance(double lat1, double lat2, double lon1,
                                  double lon2, double el1, double el2) {

        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = el1 - el2;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }
}
