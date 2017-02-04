package com.example.quad2.pp1app;

import android.app.Application;
import android.os.Build;
import android.os.StrictMode;

import com.facebook.accountkit.AccountKit;
import com.digits.sdk.android.Digits;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterCore;

import io.branch.referral.Branch;
import io.fabric.sdk.android.Fabric;

/**
 * Created by quad2 on 4/2/17.
 */

public class VerifyApplication extends Application {

    // Note: Your consumer key and secret should be obfuscated in your source code before shipping.
    private static final String TWITTER_KEY = "WcTqC5tEJaSEYVHBhpYgt5L3s";
    private static final String TWITTER_SECRET = "wRle9r9x27w754OhdaZNVcHFA7fN9CaxTCD2KPlDWkOQLKWSCR";

    @Override
    public void onCreate() {
        super.onCreate();
        TwitterAuthConfig authConfig = new TwitterAuthConfig(TWITTER_KEY, TWITTER_SECRET);
        Fabric.with(this, new TwitterCore(authConfig), new Digits.Builder().build());
        // Initialize Branch automatic session tracking
        Branch.getAutoInstance(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Kitkat and lower has a bug that can cause in correct strict mode
            // warnings about expected activity counts
            enableStrictMode();
        }




        AccountKit.initialize(getApplicationContext());
    }

    public void enableStrictMode() {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .penaltyDeath()
                .build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .penaltyDeath()
                .build());
    }
}
