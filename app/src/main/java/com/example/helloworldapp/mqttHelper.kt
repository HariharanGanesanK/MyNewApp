package com.example.helloworldapp

import android.util.Log
import org.eclipse.paho.client.mqttv3.*

class MqttHelper(serverIp: String) {

    private val brokerUrl = "tcp://$serverIp:1883"
    private val clientId = MqttClient.generateClientId()

    private val client = MqttAsyncClient(brokerUrl, clientId, null)

    fun connect(onConnected: () -> Unit = {}) {
        val options = MqttConnectOptions().apply {
            isCleanSession = true
            isAutomaticReconnect = true
        }

        client.connect(options, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d("MQTT", "Connected to broker")
                onConnected()
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.e("MQTT", "Failed to connect: ${exception?.message}")
            }
        })
    }

    fun subscribe(topic: String, callback: (String) -> Unit) {
        client.setCallback(object : MqttCallback {
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                callback(message.toString())
            }

            override fun connectionLost(cause: Throwable?) {
                Log.e("MQTT", "Connection lost: ${cause?.message}")
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {}
        })

        client.subscribe(topic, 1)
    }
}
