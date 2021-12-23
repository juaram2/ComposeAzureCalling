package com.example.composeazurecalling.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CallSetupViewModel: ViewModel() {

    //region callSetup properties
    val isVideoChecked = MutableLiveData<Boolean>(false)

    val isMicChecked = MutableLiveData<Boolean>(false)

    val isDeviceChecked = MutableLiveData<Boolean>(false)

    // Meeting State
    val displayName = MutableLiveData<String?>(null)

    private val _isValid = MutableLiveData<Boolean>(true)
    val isValid: LiveData<Boolean> = _isValid

    private val _startCall = MutableLiveData<Boolean>(false)
    val startCall: LiveData<Boolean> = _startCall
    //endregion

    //region Public Methods
    fun setDisplayName(displayName: String?) {
        this.displayName.postValue(displayName!!)
    }

    fun startCall() {
        _startCall.postValue(true)
    }
    //endregion
}