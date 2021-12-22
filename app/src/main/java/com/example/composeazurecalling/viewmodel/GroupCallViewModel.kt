package com.example.composeazurecalling.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class GroupCallViewModel() : ViewModel() {

    private val _configureChanged = MutableLiveData(false)
    val configureChanged: LiveData<Boolean> = _configureChanged

    fun setConfigureChanged(changed : Boolean) {
        _configureChanged.postValue(changed)
    }
}