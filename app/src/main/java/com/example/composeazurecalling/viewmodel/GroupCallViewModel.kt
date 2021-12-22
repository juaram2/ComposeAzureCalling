package com.example.composeazurecalling.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class GroupCallViewModel(application: Application) : ViewModel() {

    private val _configureChanged = MutableLiveData(false)
    val configureChanged: LiveData<Boolean> = _configureChanged

    fun setConfigureChanged(changed : Boolean) {
        _configureChanged.postValue(changed)
    }
}