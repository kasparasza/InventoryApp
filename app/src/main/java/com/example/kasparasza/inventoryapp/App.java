package com.example.kasparasza.inventoryapp;

import android.app.Application;

import com.facebook.stetho.Stetho;

/**
 * Class that is used to implement Stetho dependency
 */

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // initialization of Stetho
        Stetho.initializeWithDefaults(this);

        // Create an InitializerBuilder
        Stetho.InitializerBuilder initializerBuilder =
                Stetho.newInitializerBuilder(this);

        // Enable Chrome DevTools
        initializerBuilder.enableWebKitInspector(
                Stetho.defaultInspectorModulesProvider(this)
        );

        // Enable command line interface
        initializerBuilder.enableDumpapp(
                Stetho.defaultDumperPluginsProvider(getApplicationContext())
        );

        // Use the InitializerBuilder to generate an Initializer
        Stetho.Initializer initializer = initializerBuilder.build();

        // Initialize Stetho with the Initializer
        Stetho.initialize(initializer);
    }
}
