package com.android.app1.ui.main;

import android.content.Context;

import androidx.arch.core.util.Function;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

public class PageViewModel extends ViewModel {
    private Context context;

    private MutableLiveData<Integer> mIndex = new MutableLiveData<>();;

    public PageViewModel(Context context) {
        this.context = context;
    }

    private LiveData<String> mText = Transformations.map(mIndex, new Function<Integer, String>() {
        @Override
        public String apply(Integer input) {
            return String.valueOf(input);
        }
    });

    void setIndex(int index) {
        mIndex.setValue(index);
    }

    LiveData<String> getText() {
        return mText;
    }
}