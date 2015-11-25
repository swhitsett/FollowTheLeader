package com.example.sammy.followtheleader;

import android.app.Application;

import com.parse.Parse;

/**
 * Created by sammy on 11/24/15.
 */
public class MrApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Parse.enableLocalDatastore(this);
        Parse.initialize(this, "kg6d6QP0IQPIRALoiioW22RgHkzk8586Xvgwdyjh", "L9szZ1U1rxVW07SVW7Wucg3ek9u4DRE46PryrJfg");
    }
}
