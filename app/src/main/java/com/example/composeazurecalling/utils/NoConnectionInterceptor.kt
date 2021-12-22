package com.example.composeazurecalling.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
//import com.cloudhospital.CloudHospitalApp
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

class NoConnectionInterceptor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        return if(!isInternetAvailable()) {
            throw NoInternetException()
        } else {
            chain.proceed(chain.request())
        }
    }

//    private fun isConnectionOn(): Boolean {
////        val context = CloudHospitalApp.context
////        val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
////            val network = connectivityManager.activeNetwork
////            val connection = connectivityManager.getNetworkCapabilities(network)
////            return connection != null && (
////                    connection.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
////                            connection.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
//        } else {
////            val activeNetwork = connectivityManager.activeNetworkInfo
////            if (activeNetwork != null) {
////                return (activeNetwork.type == ConnectivityManager.TYPE_WIFI ||
////                        activeNetwork.type == ConnectivityManager.TYPE_MOBILE)
////            }
////            return false
//        }
//    }

    private fun isInternetAvailable(): Boolean {
        return try {
            val timeoutMs = 1500
            val sock = Socket()
            val sockaddr = InetSocketAddress("8.8.8.8", 53)

            sock.connect(sockaddr, timeoutMs)
            sock.close()

            true
        } catch (e: IOException) {
            false
        }

    }

    class NoConnectivityException : IOException() {
        override val message: String
            get() = "No network available, please check your WiFi or Data connection"
    }

    class NoInternetException() : IOException() {
        override val message: String
            get() = "No internet available, please check your connected WIFi or Data"
    }
}