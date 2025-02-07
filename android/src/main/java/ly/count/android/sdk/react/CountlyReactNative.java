package ly.count.android.sdk.react;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.util.Base64;
import android.widget.Toast;
import android.util.Log;

import android.os.Environment;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.JavaScriptModule;

import android.provider.MediaStore.Images.Media;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.HashMap;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import ly.count.android.sdk.Countly;
import ly.count.android.sdk.RemoteConfig;
import ly.count.android.sdk.DeviceId;
// import ly.count.android.sdknative.CountlyNative;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

// Push Related imports
import android.support.annotation.NonNull;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import ly.count.android.sdk.messaging.CountlyPush;

// for debug logging
import static ly.count.android.sdk.Countly.TAG;

public class CountlyReactNative extends ReactContextBaseJavaModule {
	private ReactApplicationContext _reactContext;

    private final Set<String> validConsentFeatureNames = new HashSet<String>(Arrays.asList(
            Countly.CountlyFeatureNames.sessions,
            Countly.CountlyFeatureNames.events,
            Countly.CountlyFeatureNames.views,
            Countly.CountlyFeatureNames.location,
            Countly.CountlyFeatureNames.crashes,
            Countly.CountlyFeatureNames.attribution,
            Countly.CountlyFeatureNames.users,
            Countly.CountlyFeatureNames.push,
            Countly.CountlyFeatureNames.starRating
    ));

    public CountlyReactNative(ReactApplicationContext reactContext) {
        super(reactContext);
        _reactContext = reactContext;
    }
    @Override
    public String getName() {
        return "CountlyReactNative";
    }

    public List<Class<? extends JavaScriptModule>> createJSModules() {
        return Collections.emptyList();
    }

	@ReactMethod
	public void init(ReadableArray args){
        Log.d(Countly.TAG, "Initializing...");
        String serverUrl = args.getString(0);
        String appKey = args.getString(1);
        String deviceId = args.getString(2);
        String ratingTitle = args.getString(4);
        String ratingMessage = args.getString(5);
        String ratingButton = args.getString(6);
        Boolean consentFlag = args.getBoolean(7);
        int ratingLimit = Integer.parseInt(args.getString(3));
        if("".equals(deviceId)){
            deviceId = null;
        }
        Countly.sharedInstance().setRequiresConsent(consentFlag);
        Countly.sharedInstance()
                .init(_reactContext, serverUrl, appKey, deviceId, DeviceId.Type.OPEN_UDID, ratingLimit, null, ratingTitle, ratingMessage, ratingButton);
 	}

	@ReactMethod
	public void setupPush(Integer messagingMode, ReadableMap options){
        String channelName = "Countly Notifications";
        if (options.hasKey("channelName")) {
           channelName = options.getString("channelName");
        }
        String channelDescription = "<![CDATA[Notifications from Countly React Bridge App]]>";
        if (options.hasKey("channelDescription")) {
           channelDescription = options.getString("channelDescription");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = (NotificationManager) _reactContext.getSystemService(_reactContext.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                // Create the NotificationChannel
                NotificationChannel channel = new NotificationChannel(CountlyPush.CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription(channelDescription);
                notificationManager.createNotificationChannel(channel);
            }
        }

        Countly.sharedInstance()
                .setRequiresConsent(true)
                .setConsent(new String[]{Countly.CountlyFeatureNames.push, Countly.CountlyFeatureNames.sessions, Countly.CountlyFeatureNames.location, Countly.CountlyFeatureNames.attribution, Countly.CountlyFeatureNames.crashes, Countly.CountlyFeatureNames.events, Countly.CountlyFeatureNames.starRating, Countly.CountlyFeatureNames.users, Countly.CountlyFeatureNames.views}, true)
                .setLoggingEnabled(true)
                .setPushIntentAddMetadata(true);
                // .init(this, serverUrl, appKey);

        final Activity activity = getCurrentActivity();
        Countly.CountlyMessagingMode mode = Countly.CountlyMessagingMode.TEST;
        if (messagingMode == 0) {
           mode = Countly.CountlyMessagingMode.PRODUCTION;
        }
        CountlyPush.init(activity.getApplication(), mode);


        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "getInstanceId failed", task.getException());
                            return;
                        }

                        // Get new Instance ID token
                        String token = task.getResult().getToken();
                        CountlyPush.onTokenRefresh(token);
                    }
                });
    }

	@ReactMethod
	public void setLoggingEnabled(ReadableArray args){
        Boolean enabled = args.getBoolean(0);
        Countly.sharedInstance().setLoggingEnabled(enabled);
	}

    @ReactMethod
    public Boolean isInitialized(ReadableArray args){
        return Countly.sharedInstance().isInitialized();
    }

    @ReactMethod
    public Boolean hasBeenCalledOnStart(ReadableArray args){
        return Countly.sharedInstance().hasBeenCalledOnStart();
    }

    @ReactMethod
    public void changeDeviceId(ReadableArray args){
        String newDeviceID = args.getString(0);
        String onServerString = args.getString(1);
        if("1".equals(onServerString)){
            Countly.sharedInstance().changeDeviceId(newDeviceID);
        }else{
            Countly.sharedInstance().changeDeviceId(DeviceId.Type.DEVELOPER_SUPPLIED, newDeviceID);
        }
    }

    @ReactMethod
    public void setHttpPostForced(ReadableArray args){
        int isEnabled = Integer.parseInt(args.getString(0));
        if(isEnabled == 1){
            Countly.sharedInstance().setHttpPostForced(true);
        }else{
            Countly.sharedInstance().setHttpPostForced(false);
        }
    }

    @ReactMethod
    public void enableParameterTamperingProtection(ReadableArray args){
        String salt = args.getString(0);
        Countly.sharedInstance().enableParameterTamperingProtection(salt);
    }

    @ReactMethod
    public void setLocation(ReadableArray args){
        String countryCode = args.getString(0);
        String city = args.getString(1);
        String location = args.getString(2);
        String ipAddress = args.getString(3);
        if("".equals(countryCode)){
            countryCode = null;
        }
        if("".equals(city)){
            city = null;
        }
        if("0.0.0.0".equals(ipAddress)){
            ipAddress = null;
        }
        if("0.0,0.0".equals(location)){
            location = null;
        }
        Countly.sharedInstance().setLocation(countryCode, city, location, ipAddress);
    }

    @ReactMethod
    public void disableLocation(){
        Countly.sharedInstance().disableLocation();
    }

    @ReactMethod
    public void enableCrashReporting(){
        Countly.sharedInstance().enableCrashReporting();
    }

    @ReactMethod
    public void addCrashLog(ReadableArray args){
        String record = args.getString(0);
        Countly.sharedInstance().addCrashLog(record);
    }
    @ReactMethod
    public void logException(ReadableArray args){
        String exceptionString = args.getString(0);
        Exception exception = new Exception(exceptionString);

        Boolean nonfatal = args.getBoolean(1);

        HashMap<String, String> segments = new HashMap<String, String>();
        for(int i=2,il=args.size();i<il;i+=2){
            segments.put(args.getString(i), args.getString(i+1));
        }
        segments.put("nonfatal", nonfatal.toString());
        Countly.sharedInstance().setCustomCrashSegments(segments);

        Countly.sharedInstance().logException(exception);
    }

    @ReactMethod
    public void setCustomCrashSegments(ReadableArray args){
        Map<String, String> segments = null;
        for(int i=0,il=args.size();i<il;i++){
            segments.put(args.getString(i), args.getString(i));
        }
        Countly.sharedInstance().setCustomCrashSegments(segments);
    }

   @ReactMethod
    public void event(ReadableArray args){
        String eventType = args.getString(0);
        if("event".equals(eventType)){
            String eventName = args.getString(1);
            int eventCount= Integer.parseInt(args.getString(2));
            Countly.sharedInstance().recordEvent(eventName, eventCount);
        }
        else if ("eventWithSum".equals(eventType)) {
            String eventName = args.getString(1);
            int eventCount= Integer.parseInt(args.getString(2));
            float eventSum= new Float(args.getString(3)).floatValue();
            Countly.sharedInstance().recordEvent(eventName, eventCount, eventSum);
        }
        else if ("eventWithSegment".equals(eventType)) {
            String eventName = args.getString(1);
            int eventCount= Integer.parseInt(args.getString(2));
            HashMap<String, String> segmentation = new HashMap<String, String>();
            for(int i=3,il=args.size();i<il;i+=2){
                segmentation.put(args.getString(i), args.getString(i+1));
            }
            Countly.sharedInstance().recordEvent(eventName, segmentation, eventCount);
            }
        else if ("eventWithSumSegment".equals(eventType)) {
            String eventName = args.getString(1);
            int eventCount= Integer.parseInt(args.getString(2));
            float eventSum= new Float(args.getString(3)).floatValue();
            HashMap<String, String> segmentation = new HashMap<String, String>();
            for(int i=4,il=args.size();i<il;i+=2){
                segmentation.put(args.getString(i), args.getString(i+1));
            }
            Countly.sharedInstance().recordEvent(eventName, segmentation, eventCount,eventSum);
        }
        else{
        }
    }

    @ReactMethod
    public void startEvent(ReadableArray args){
        String startEvent = args.getString(0);
        Countly.sharedInstance().startEvent(startEvent);
    }

    @ReactMethod
    public void endEvent(ReadableArray args){
        String eventType = args.getString(0);
        if("event".equals(eventType)){
            String eventName = args.getString(1);
            Countly.sharedInstance().endEvent(eventName);
        }
        else if ("eventWithSum".equals(eventType)) {
            String eventName = args.getString(1);
            int eventCount= Integer.parseInt(args.getString(2));
            float eventSum= new Float(args.getString(3)).floatValue();
            Countly.sharedInstance().endEvent(eventName, null, eventCount,eventSum);
        }
        else if ("eventWithSegment".equals(eventType)) {
            String eventName = args.getString(1);
            int eventCount= Integer.parseInt(args.getString(2));
            HashMap<String, String> segmentation = new HashMap<String, String>();
            for(int i=4,il=args.size();i<il;i+=2){
                segmentation.put(args.getString(i), args.getString(i+1));
            }
            Countly.sharedInstance().endEvent(eventName, segmentation, eventCount,0);
        }
        else if ("eventWithSumSegment".equals(eventType)) {
            String eventName = args.getString(1);
            int eventCount= Integer.parseInt(args.getString(2));
            float eventSum= new Float(args.getString(3)).floatValue();
            HashMap<String, String> segmentation = new HashMap<String, String>();
            for(int i=4,il=args.size();i<il;i+=2){
                segmentation.put(args.getString(i), args.getString(i+1));
            }
            Countly.sharedInstance().endEvent(eventName, segmentation, eventCount,eventSum);
        }
        else{
        }
    }

	@ReactMethod
	public void recordView(ReadableArray args){
        String viewName = args.getString(0);
		Countly.sharedInstance().recordView(viewName);
    }

    @ReactMethod
    public void setViewTracking(ReadableArray args){
        String flag = args.getString(0);
        if("true".equals(flag)){
            Countly.sharedInstance().setViewTracking(true);
        }else{
            Countly.sharedInstance().setViewTracking(false);
        }
    }




	@ReactMethod
	public void setUserData(ReadableArray args){
        Map<String, String> bundle = new HashMap<String, String>();
        bundle.put("name", args.getString(0));
        bundle.put("username", args.getString(1));
        bundle.put("email", args.getString(2));
        bundle.put("org", args.getString(3));
        bundle.put("phone", args.getString(4));
        bundle.put("picture", args.getString(5));
        bundle.put("picturePath", args.getString(6));
        bundle.put("gender", args.getString(7));
        bundle.put("byear", String.valueOf(args.getInt(8)));
        Countly.userData.setUserData(bundle);
        Countly.userData.save();
	}

	@ReactMethod
	 public void onRegistrationId(ReadableArray args){
        String pushToken = args.getString(0);
        int messagingMode = Integer.parseInt(args.getString(1));

        Countly.CountlyMessagingMode mode = null;
        if(messagingMode == 0){
            mode = Countly.CountlyMessagingMode.PRODUCTION;
        }
        else{
            mode = Countly.CountlyMessagingMode.TEST;
        }
        Countly.sharedInstance().onRegistrationId(pushToken, mode);
	}

	@ReactMethod
	public void start(){
		Countly.sharedInstance().onStart(getCurrentActivity());
	}

	@ReactMethod
	public void stop(){
		Countly.sharedInstance().onStop();
	}

    @ReactMethod
    public void userData_setProperty(ReadableArray args){
        String keyName = args.getString(0);
        String keyValue = args.getString(1);
        Countly.userData.setProperty(keyName, keyValue);
        Countly.userData.save();
    }

    @ReactMethod
    public void userData_increment(ReadableArray args){
        String keyName = args.getString(0);
        Countly.userData.increment(keyName);
        Countly.userData.save();
    }

    @ReactMethod
    public void userData_incrementBy(ReadableArray args){
        String keyName = args.getString(0);
        int keyIncrement = Integer.parseInt(args.getString(1));
        Countly.userData.incrementBy(keyName, keyIncrement);
        Countly.userData.save();
    }

    @ReactMethod
    public void userData_multiply(ReadableArray args){
        String keyName = args.getString(0);
        int multiplyValue = Integer.parseInt(args.getString(1));
        Countly.userData.multiply(keyName, multiplyValue);
        Countly.userData.save();
    }

    @ReactMethod
    public void userData_saveMax(ReadableArray args){
        String keyName = args.getString(0);
        int maxScore = Integer.parseInt(args.getString(1));
        Countly.userData.saveMax(keyName, maxScore);
        Countly.userData.save();
    }

    @ReactMethod
    public void userData_saveMin(ReadableArray args){
        String keyName = args.getString(0);
        int minScore = Integer.parseInt(args.getString(1));
        Countly.userData.saveMin(keyName, minScore);
        Countly.userData.save();
    }

    @ReactMethod
    public void userData_setOnce(ReadableArray args){
        String keyName = args.getString(0);
        String minScore = args.getString(1);
        Countly.userData.setOnce(keyName, minScore);
        Countly.userData.save();
    }

    @ReactMethod
    public void userData_pushUniqueValue(ReadableArray args){
        String keyName = args.getString(0);
        String keyValue = args.getString(1);
        Countly.userData.pushUniqueValue(keyName, keyValue);
        Countly.userData.save();
    }

    @ReactMethod
    public void userData_pushValue(ReadableArray args){
        String keyName = args.getString(0);
        String keyValue = args.getString(1);
        Countly.userData.pushValue(keyName, keyValue);
        Countly.userData.save();
    }

    @ReactMethod
    public void userData_pullValue(ReadableArray args){
        String keyName = args.getString(0);
        String keyValue = args.getString(1);
        Countly.userData.pullValue(keyName, keyValue);
        Countly.userData.save();
    }

    // GDPR
    @ReactMethod
    public void setRequiresConsent(ReadableArray args){
			  Boolean consentFlag = args.getBoolean(0);
        Countly.sharedInstance().setRequiresConsent(consentFlag);
    }

    @ReactMethod
    public void giveConsent(ReadableArray featureNames){
        List<String> features = new ArrayList<>();
        for (int i = 0; i < featureNames.size(); i++) {
            String featureName = featureNames.getString(i);
            if (validConsentFeatureNames.contains(featureName)) {
               features.add(featureName);
            }
            else {
               Log.d(Countly.TAG, "Not a valid consent feature to add: " + featureName);
            }
        }
        Countly.sharedInstance().giveConsent(features.toArray(new String[features.size()]));
    }

    @ReactMethod
    public void removeConsent(ReadableArray featureNames){
        List<String> features = new ArrayList<>();
        for (int i = 0; i < featureNames.size(); i++) {
            String featureName = featureNames.getString(i);
            if (validConsentFeatureNames.contains(featureName)) {
               features.add(featureName);
            }
            else {
               Log.d(Countly.TAG, "Not a valid consent feature to remove: " + featureName);
            }
        }
        Countly.sharedInstance().removeConsent(features.toArray(new String[features.size()]));
    }

    @ReactMethod
    public void giveAllConsent(){
        Countly.sharedInstance().giveConsent(validConsentFeatureNames.toArray(new String[validConsentFeatureNames.size()]));
    }

    @ReactMethod
    public void removeAllConsent(){
        Countly.sharedInstance().removeConsent(validConsentFeatureNames.toArray(new String[validConsentFeatureNames.size()]));
    }


    @ReactMethod
    public void remoteConfigUpdate(ReadableArray args, final Callback myCallback){
        Countly.sharedInstance().remoteConfigUpdate(new RemoteConfig.RemoteConfigCallback() {
            String resultString = "";
            @Override
            public void callback(String error) {
                if(error == null) {
                    resultString = "Remote Config is updated and ready to use!";
                } else {
                    resultString = "There was an error while updating Remote Config: " + error;
                }
                myCallback.invoke(resultString);
            }
        });
    }

    @ReactMethod
    public void updateRemoteConfigForKeysOnly(ReadableArray args, final Callback myCallback){
        int i = args.size();
        int n = ++i;
        String[] newArray = new String[n];
        for(int cnt=0;cnt<args.size();cnt++)
        {
            newArray[cnt] = args.getString(cnt);
        }
        Countly.sharedInstance().updateRemoteConfigForKeysOnly(newArray, new RemoteConfig.RemoteConfigCallback() {
            String resultString = "";
            @Override
            public void callback(String error) {
                if(error == null) {
                    resultString = "Remote Config is updated only for given keys and ready to use!";
                } else {
                    resultString = "There was an error while updating Remote Config: " + error;
                }
                myCallback.invoke(resultString);
            }
        });
    }


    @ReactMethod
    public void updateRemoteConfigExceptKeys(ReadableArray args, final Callback myCallback){
        int i = args.size();
        int n = ++i;
        String[] newArray = new String[n];
        for(int cnt=0;cnt<args.size();cnt++)
        {
            newArray[cnt] = args.getString(cnt);
        }
        Countly.sharedInstance().updateRemoteConfigExceptKeys(newArray, new RemoteConfig.RemoteConfigCallback() {
            String resultString = "";
            @Override
            public void callback(String error) {
                if (error == null) {
                    resultString = "Remote Config is updated except for given keys and ready to use !";
                } else {
                    resultString = "There was an error while updating Remote Config: " + error;
                }
                myCallback.invoke(resultString);
            }
        });
    }

    @ReactMethod
    public void getRemoteConfigValueForKey(ReadableArray args, final Callback myCallback){
        String keyName = args.getString(0);
        Object keyValue = Countly.sharedInstance().getRemoteConfigValueForKey(keyName);
        if (keyValue == null) {
            // Log.d(TAG, keyName + ": ConfigKeyNotFound");
            myCallback.invoke("ConfigKeyNotFound");
        }
        else {
            String resultString = (keyValue).toString();
            // Log.d(TAG, keyName + ": " + resultString);
            myCallback.invoke(resultString);
        }
    }

    @ReactMethod
    public void getRemoteConfigValueForKeyP(String keyName, Promise promise){
        Object keyValue = Countly.sharedInstance().getRemoteConfigValueForKey(keyName);
        if (keyValue == null) {
            Log.d(TAG, keyName + ": ConfigKeyNotFound");
            promise.reject("ConfigKeyNotFound");
        }
        else {
            String resultString = (keyValue).toString();
            Log.d(TAG, keyName + ": " + resultString);
            promise.resolve(resultString);
        }
    }

    @ReactMethod
    public void remoteConfigClearValues(Promise promise){
        Countly.sharedInstance().remoteConfigClearValues();
        promise.resolve("Remote Config Cleared.");
    }

    @ReactMethod
    public void showStarRating(ReadableArray args){
        Activity activity = getCurrentActivity();
        Countly.sharedInstance().showStarRating(activity, null);

    }

    @ReactMethod
    public void showFeedbackPopup(ReadableArray args){
        String widgetId = args.getString(0);
        String closeFeedBackButton = args.getString(1);
        Activity activity = getCurrentActivity();
        Countly.sharedInstance().showFeedbackPopup( widgetId, closeFeedBackButton, activity, null);
    }

    @ReactMethod
    public void setEventSendThreshold(ReadableArray args){
        int size = Integer.parseInt(args.getString(0));
        Countly.sharedInstance().setEventQueueSizeToSend(size);
    }

    /*
    @ReactMethod
    public void initNative(){
            CountlyNative.initNative(getReactApplicationContext());
    }

    @ReactMethod
    public void testCrash(){
            CountlyNative.crash();
    }
    */

}
