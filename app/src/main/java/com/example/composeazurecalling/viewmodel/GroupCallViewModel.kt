package com.example.composeazurecalling.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class GroupCallViewModel(application: Application) : ViewModel() {
    private val _context = application.applicationContext

    private val _configureChanged = MutableLiveData<Boolean>(false)
    val configureChanged: LiveData<Boolean> = _configureChanged

    fun setConfigureChaned(changed : Boolean) {
        _configureChanged.postValue(changed)
    }
}