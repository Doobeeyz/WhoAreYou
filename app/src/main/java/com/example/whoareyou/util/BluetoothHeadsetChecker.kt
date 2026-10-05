package com.example.whoareyou.util

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat

class BluetoothHeadsetChecker(private val context: Context) {
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun isHeadsetConnected(): Boolean {
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED){
            return false
        }
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter: BluetoothAdapter? = bluetoothManager.adapter
        if (adapter != null) {
            if (adapter.isEnabled && (adapter.getProfileConnectionState(BluetoothProfile
                    .HEADSET) == BluetoothAdapter.STATE_CONNECTED || adapter.getProfileConnectionState(BluetoothProfile.A2DP) == BluetoothAdapter.STATE_CONNECTED)){
                return true
            }
        }
        return false
    }
}