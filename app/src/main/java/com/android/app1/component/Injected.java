package com.android.app1.component;

import android.content.Context;

import androidx.lifecycle.ViewModelProvider;

public class Injected {
    public static ViewModelProviderFactory viewModelFactory(Context context){
        return new ViewModelProviderFactory(context);
    }
}
