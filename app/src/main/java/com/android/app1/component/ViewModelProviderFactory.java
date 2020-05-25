package com.android.app1.component;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.android.app1.ui.main.PageViewModel;

public class ViewModelProviderFactory implements ViewModelProvider.Factory {

    @NonNull
    private Context context;

    public ViewModelProviderFactory(@NonNull Context context) {
        this.context = context;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if(modelClass.isAssignableFrom(PageViewModel.class)){
            return (T) new PageViewModel(context);
        }else{
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}
