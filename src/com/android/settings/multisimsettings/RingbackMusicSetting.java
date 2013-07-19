package com.android.settings.multisimsettings;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Toast;
/*xieyunyang.hoperun 2012.7.4 Send text messages and listen to the SMS sending state*/
public class RingbackMusicSetting extends Activity {

	// send and receive broadcast
	String SENT_SMS_ACTION = "SENT_SMS_ACTION";
	String DELIVERED_SMS_ACTION = "DELIVERED_SMS_ACTION";
	Context mContext = null;
	private SendMessage mReceiver;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		if (mReceiver == null) {
			mReceiver = new SendMessage();
		}
		registerReceiver(mReceiver, new IntentFilter(SENT_SMS_ACTION));
		registerReceiver(mReceiver, new IntentFilter(DELIVERED_SMS_ACTION));

		SmsManager sms = SmsManager.getDefault();
		Intent sentIntent = new Intent(SENT_SMS_ACTION);
		PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, sentIntent,
				0);

		// create the deilverIntent parameter
		Intent deliverIntent = new Intent(DELIVERED_SMS_ACTION);
		PendingIntent deliverPI = PendingIntent.getBroadcast(this, 0,
				deliverIntent, 0);
		sms.sendTextMessage("118100", null, "sla", sentPI, deliverPI);
		Toast.makeText(RingbackMusicSetting.this, "sending...",
				Toast.LENGTH_LONG).show();

	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(mReceiver);
		super.onDestroy();
	}

	public class SendMessage extends BroadcastReceiver {

		// String action_get;

		@Override
		public void onReceive(Context context, Intent intent) {
			switch (getResultCode()) {
			case Activity.RESULT_OK:
				Toast.makeText(context, "短信发送成功", Toast.LENGTH_SHORT).show();
				RingbackMusicSetting.this.finish();
				break;

			default:
				Toast.makeText(mContext, "发送失败", Toast.LENGTH_LONG).show();
				RingbackMusicSetting.this.finish();
				break;

			}

		}

	}

}
/*xieyunyang.hoperun 2012.7.4 end*/
