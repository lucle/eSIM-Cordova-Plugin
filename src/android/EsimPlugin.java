package com.dreamcloud;
// The native android API
import android.telephony.euicc.EuiccManager;
import android.telephony.euicc.DownloadableSubscription;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.app.PendingIntent;
import android.util.Log;
// Cordova-required packages
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class EsimPlugin extends CordovaPlugin {
    protected static final String LOG_TAG = "eSIM";
    private static final String HAS_ESIM_ENABLED = "hasEsimEnabled";
    private String ACTION_DOWNLOAD_SUBSCRIPTION = "download_subscription";
    Context mainContext;
    EuiccManager mgr;

     // at the initialize function, we can configure the tools we want to use later, like the sensors
     @Override
     public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        mainContext = this.cordova.getActivity().getApplicationContext();
        Log.d(LOG_TAG, "initialize()");
     }
    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        try {
            if (HAS_ESIM_ENABLED.equals(action)) {
                Log.d(LOG_TAG, "checking eSIM support");
                hasEsimEnabled(callbackContext);
            }else if (ACTION_DOWNLOAD_SUBSCRIPTION.equals(action)) {   
                Log.d(LOG_TAG, "install eSIM");     
                installEsim(args, callbackContext);
            }else{
                return false;
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error execute "  + e.getMessage());
            callbackContext.sendPluginResult(new PluginResult(Status.ERROR));
            return false;
        }
        return true;
    }
    private void initMgr() {
        if (mgr == null) {
          mgr = (EuiccManager) mainContext.getSystemService(Context.EUICC_SERVICE);
        }
    }
    private void hasEsimEnabled(CallbackContext callbackContext) {
        initMgr();
        boolean result = mgr.isEnabled();
        callbackContext.sendPluginResult(new PluginResult(Status.OK, result));
    }
    private void installEsim(JSONArray args, CallbackContext callbackContext) throws JSONException{
        initMgr();
        // Register receiver.
        String LPA_DECLARED_PERMISSION = args.getString(0);
        String activationCode = args.getString(1);
        Log.d(LOG_TAG, "activationCode = " + activationCode + "\n LPA_DECLARED_PERMISSION: " + LPA_DECLARED_PERMISSION);
        try{
            BroadcastReceiver receiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            if (!ACTION_DOWNLOAD_SUBSCRIPTION.equals(intent.getAction())) {
                                return;
                            }
                            int resultCode = getResultCode();
                            // If the result code is a resolvable error, call startResolutionActivity
                            if (resultCode == EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_RESOLVABLE_ERROR) {
                                try {
                                    PendingIntent callbackIntent = PendingIntent.getBroadcast(mainContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
                                    mgr.startResolutionActivity(cordova.getActivity(), 0, intent, callbackIntent);
                                } catch (Exception e) {  
                                    Log.e(LOG_TAG, "Error startResolutionActivity "  + e.getMessage());        
                                    callbackContext.error(e.getMessage());    
                                    callbackContext.sendPluginResult(new PluginResult(Status.ERROR));                 
                                }
                            } else if (resultCode == EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_ERROR) {
                                // Embedded Subscription Error
                                Log.e(LOG_TAG, "EMBEDDED_SUBSCRIPTION_RESULT_ERROR - Can't add an Esim subscription");        
                                callbackContext.error("EMBEDDED_SUBSCRIPTION_RESULT_ERROR - Can't add an Esim subscription");  
                              } 
                            Intent resultIntent = intent;
                        }
                    };
            mainContext.registerReceiver(receiver, new IntentFilter(ACTION_DOWNLOAD_SUBSCRIPTION), null, null);

            // Download subscription asynchronously.
            DownloadableSubscription sub = DownloadableSubscription.forActivationCode(activationCode);
            Intent intent = new Intent(ACTION_DOWNLOAD_SUBSCRIPTION);
            PendingIntent callbackIntent = PendingIntent.getBroadcast(mainContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
            mgr.downloadSubscription(sub, true, callbackIntent);
            callbackContext.sendPluginResult(new PluginResult(Status.OK, "success"));
        }catch (Exception e) {
            Log.e(LOG_TAG, "Error install eSIM "  + e.getMessage());
            callbackContext.error(e.getMessage());
            callbackContext.sendPluginResult(new PluginResult(Status.ERROR));
        }
    }       
}
