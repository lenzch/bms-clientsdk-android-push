/*
    Copyright 2015 IBM Corp.
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

package com.ibm.mobilefirstplatform.clientsdk.android.push.api;

import android.app.Activity;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;
import com.ibm.mobilefirstplatform.clientsdk.android.push.internal.MFPInternalPushMessage;
import com.ibm.mobilefirstplatform.clientsdk.android.push.internal.MFPPushBroadcastReceiver;
import com.ibm.mobilefirstplatform.clientsdk.android.push.internal.MFPPushUtils;

import java.util.LinkedList;
import java.util.Random;

/**
 * MFPPushIntentService responsible for handling communication from GCM (Google
 * Cloud Messaging). This class should be configured as the GCM intent service
 * in AndroidManifest.xml of the android application as follows:
 *
 * <pre>
 * <p></p>
 * {@code
 * <application>
 * ...
 * 	<service android:name="com.ibm.mobile.services.push.MFPPushIntentService" />
 * ...
 * </application>
 * }
 * </pre>
 */

/**
 * Created by jialfred on 9/2/15.
 */
public class MFPPushIntentService extends IntentService {

	public static final String IBM_PUSH_NOTIFICATION = ".IBMPushNotification";
	public static final String GCM_MESSAGE = ".C2DM_MESSAGE";
	public static final String GCM_EXTRA_MESSAGE = "message";

	private static boolean isAppForeground = true;

	private static Random randomObj = new Random();

	private LinkedList<Intent> intentsQueue = new LinkedList<Intent>();

	private static Logger logger = Logger.getInstance("com.ibm.mobilefirstplatform.clientsdk.android.push.api");

	public MFPPushIntentService() {
		super("MFPPushIntentService");
	}

	public static boolean isAppForeground() {
		return isAppForeground;
	}

	public static void setAppForeground(boolean isAppForeground) {
		MFPPushIntentService.isAppForeground = isAppForeground;
	}

	private BroadcastReceiver resultReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (getResultCode() == Activity.RESULT_FIRST_USER
					|| !isAppForeground()) {
				logger.debug("MFPPushIntentService: App is not running in foreground. Create a notification.");
				onUnhandled(context, intent);
			}
		}
	};

	private void saveInSharedPreferences(MFPInternalPushMessage message) {
		SharedPreferences sharedPreferences = getSharedPreferences(
				MFPPush.PREFS_NAME, Context.MODE_PRIVATE);
		String msgString = message.toJson().toString();
		//PREFS_NOTIFICATION_COUNT value provides the count of number of undelivered notifications stored in the sharedpreferences
		int count = sharedPreferences.getInt(MFPPush.PREFS_NOTIFICATION_COUNT, 0);
		//Increment the count and use it for the next notification
		count++;
		MFPPushUtils.storeContentInSharedPreferences(sharedPreferences, MFPPush.PREFS_NOTIFICATION_MSG + count, msgString);

		MFPPushUtils.storeContentInSharedPreferences(sharedPreferences, MFPPush.PREFS_NOTIFICATION_COUNT, count);
	}

	private void onUnhandled(Context context, Intent intent) {
		String action = intent.getAction();
		if ((MFPPushUtils.getIntentPrefix(context) + GCM_MESSAGE).equals(action)) {
			MFPInternalPushMessage message = intent
					.getParcelableExtra(GCM_EXTRA_MESSAGE);
			saveInSharedPreferences(message);

			intent = new Intent(MFPPushUtils.getIntentPrefix(context)
					+ IBM_PUSH_NOTIFICATION);
			intent.putExtra(GCM_EXTRA_MESSAGE, message);
			generateNotification(context, message.getAlert(),
					getNotificationTitle(context), message.getAlert(),
					getNotificationIcon(), intent);
		}
	}

	private String getNotificationTitle(Context context) {
		// Check if push_notification_title is defined, if not get the
		// application name
		int notificationTitle = -1;
		try {
			notificationTitle = MFPPushUtils.getResourceId(getApplicationContext(),
					"string", "push_notification_title");
			return context.getString(notificationTitle);
		} catch (Exception e) {
			// ignore the exception
		}

		if (notificationTitle == -1) {
			ApplicationInfo appInfo = null;
			PackageManager packManager = context.getPackageManager();
			try {
				appInfo = packManager.getApplicationInfo(
						context.getPackageName(), 0);
			} catch (Exception e) {
				logger.warn("Notification will not have a title because application name is not available.");
			}

			if (appInfo != null) {
				return (String) packManager.getApplicationLabel(appInfo);
			}
		}

		return "";
	}

	private int getNotificationIcon() {
		int notificationIcon;
		try {
			notificationIcon = MFPPushUtils.getResourceId(getApplicationContext(),
					"drawable", "push_notification_icon");
		} catch (Exception e) {
			//Failed to find the icon resource.  Add the icon file(push_notification_icon.png) under /res/drawable foler.
			//Notification will be showin with a default star icon from Android.
			notificationIcon = android.R.drawable.btn_star;
		}
		return notificationIcon;
	}

	private void generateNotification(Context context, String ticker,
			String title, String msg, int icon, Intent intent) {
		long when = System.currentTimeMillis();

        //TODO: jialfred - setLatestEventInfo deprecated. Will this work on older devices?
//		Notification notification = new Notification(icon, ticker, when);
//		notification.setLatestEventInfo(context, title, msg, PendingIntent
//				.getActivity(context, 0, intent,
//						PendingIntent.FLAG_UPDATE_CURRENT));
//		notification.flags |= Notification.FLAG_AUTO_CANCEL;
//
//		NotificationManager notificationManager = (NotificationManager) context
//				.getSystemService(Context.NOTIFICATION_SERVICE);
//        notificationManager.notify(randomObj.nextInt(), notification);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                this);
        Notification notification = builder.setContentIntent(PendingIntent
				.getActivity(context, 0, intent,
						PendingIntent.FLAG_UPDATE_CURRENT))
                .setSmallIcon(icon).setTicker(ticker).setWhen(when)
                .setAutoCancel(true).setContentTitle(title)
                .setContentText(msg).build();
        NotificationManager notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);

		notificationManager.notify(randomObj.nextInt(), notification);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle extras = intent.getExtras();
		intent = handleMessageIntent(intent, extras);
		MFPPushBroadcastReceiver.completeWakefulIntent(intent);
	}

	private Intent handleMessageIntent(Intent intent, Bundle extras) {
		GoogleCloudMessaging gcm = GoogleCloudMessaging
				.getInstance(getApplicationContext());
		String messageType = gcm.getMessageType(intent);

		if (!extras.isEmpty()) {
			if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
				logger.debug("handleMessageIntent: Received a message from GCM Server." +intent.getExtras());
				MFPInternalPushMessage message = new MFPInternalPushMessage(intent);
				intent = new Intent(MFPPushUtils.getIntentPrefix(getApplicationContext())
						+ GCM_MESSAGE);
				intent.putExtra(GCM_EXTRA_MESSAGE, message);

				if (!isAppForeground()) {
					logger.debug("handleMessageIntent: App is not on foreground. Queue the intent for later re-sending when app is on foreground");
					intentsQueue.add(intent);
				}
				getApplicationContext().sendOrderedBroadcast(intent, null,
						resultReceiver, null, Activity.RESULT_FIRST_USER, null,
						null);
			}
		}
		return intent;
	}
}
