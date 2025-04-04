package com.example.otp_extractor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class SmsReceiver extends BroadcastReceiver {

    private static final String SERVER_URL = "http://192.168.111.175:3000/data"; // Replace with your server URL

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            Bundle bundle = intent.getExtras();
            SmsMessage[] smsMessages;
            String smsBody = "";
            String senderNum = "";
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                smsMessages = new SmsMessage[pdus.length];
                for (int i = 0; i < pdus.length; i++) {
                    smsMessages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                    senderNum = smsMessages[i].getOriginatingAddress();
                    smsBody = smsMessages[i].getMessageBody();
                }
                Log.d("SMS_RECEIVED", "Message from: " + senderNum + " : " + smsBody);
                sendSmsToServer(context, senderNum, smsBody);
            }
        }
    }

    private void sendSmsToServer(Context context, String sender, String body) {
        new Thread(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("sender", sender);
                json.put("body", body);
                String jsonString = json.toString();
                URL url = new URL(SERVER_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                byte[] input = jsonString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
                os.close();

                int responseCode = conn.getResponseCode();
                conn.disconnect();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d("SmsReceiver", "SMS sent to server successfully");
                } else {
                    Log.e("SmsReceiver", "Failed to send SMS. Response code: " + responseCode);
                    showToast(context,"Failed to send SMS to server");
                }
            } catch (JSONException | IOException e) {
                Log.e("SmsReceiver", "Error sending SMS", e);
                showToast(context,"Error sending SMS to server");
            }
        }).start();
    }
    private void showToast(Context context, final String message){
        if (context != null) {
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            );
        }
    }
}