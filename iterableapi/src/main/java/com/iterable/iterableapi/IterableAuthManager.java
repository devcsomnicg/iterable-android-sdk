package com.iterable.iterableapi;

import android.util.Base64;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Timer;
import java.util.TimerTask;

public class IterableAuthManager {
    private static final String TAG = "IterableAuth";
    private String authToken;

    //For expiration handling
    private static final int refreshWindowTime = 60000;
    private static final String expirationString = "exp";

    //For rate limiting
    private boolean pendingAuth;
    private long lastTokenRequestTime;
    private long rateLimitTime = 1000;
    private Timer timer;

    private int failedSequentialAuthRequestCount;

    IterableAuthManager()
    {
        timer = new Timer();
    }

    public void requestNewAuthToken() {
        //What do we do if there's already a pending auth? Ignore since it will eventually fix itself
        if (pendingAuth == false) {
            long currentTime = IterableUtil.currentTimeMillis();
            if (currentTime - lastTokenRequestTime >= rateLimitTime) {
                pendingAuth = true;

                failedSequentialAuthRequestCount++;
                //Blocking call on a separate thread
                String authToken = "";//onAuthTokenRequested();
                updateAuthToken(this.authToken);
            } else {
                //queue up another requestNewAuthToken at currentTime + rateLimitTime
                scheduleAuthTokenRefresh(lastTokenRequestTime + rateLimitTime);
            }
            lastTokenRequestTime = currentTime;
        }
    }

    public void resetFailedAuthRequestCount() {
        failedSequentialAuthRequestCount = 0;
    }

    //TODO: do we need to make this public?
    private void updateAuthToken(String authToken) {
       if (!authToken.equals(this.authToken)) {
            this.authToken = authToken;
            pendingAuth = false;

            queueExpirationRefresh(authToken);
        }
    }

    private void queueExpirationRefresh(String JWTEncoded) {
        int expirationTimeSeconds = decodedExpiration(JWTEncoded);
        long triggerExpirationRefreshTime = expirationTimeSeconds*1000 - refreshWindowTime - IterableUtil.currentTimeMillis();
        scheduleAuthTokenRefresh(triggerExpirationRefreshTime);
    }

    private void scheduleAuthTokenRefresh(long timeDuration) {
        timer.cancel();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                requestNewAuthToken();
            }
        }, 0, timeDuration);
    }

    private int decodedExpiration(String JWTEncoded) {
        int exp = 0;
        try {
            String[] split = JWTEncoded.split("\\.");
            String body = getJson(split[1]);
            JSONObject jObj = new JSONObject(body);
            exp = jObj.getInt(expirationString);
        } catch (Exception e) {
            IterableLogger.e(TAG, "Error while parsing JWT for the expiration: " + JWTEncoded, e);
        }
        return exp;
    }

    private String getJson(String strEncoded) throws UnsupportedEncodingException {
        byte[] decodedBytes = Base64.decode(strEncoded, Base64.URL_SAFE);
        return new String(decodedBytes, "UTF-8");
    }
}