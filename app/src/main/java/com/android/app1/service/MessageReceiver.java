package com.android.app1.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

public class MessageReceiver extends BroadcastReceiver {
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    private static final String TAG = "MessageReceiver";
    public static String msg = "";
    public static String phoneNo = "";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Intent Received"+intent.getAction());
        if (intent.getAction() != null && intent.getAction().equals(SMS_RECEIVED)){
            Bundle dataBundle = intent.getExtras();
            SmsMessage[] sms;
            if (dataBundle != null){
                try{
                    Object[] pdus = (Object[])dataBundle.get("pdus");
                    if (pdus != null){
                        sms = new SmsMessage[pdus.length];
                        for (int i=0; i<sms.length; i++){
                            sms[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
                            phoneNo = sms[i].getOriginatingAddress();
                            msg = sms[i].getMessageBody();
                        }
                    }{
                        throw new Exception("No message from pdus");
                    }
                }catch (Exception e){
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        }
    }
}
