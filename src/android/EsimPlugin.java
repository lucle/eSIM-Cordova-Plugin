package com.dreamcloud;
// The native android API
import android.telephony.euicc.EuiccManager;
import android.telephony.euicc.DownloadableSubscription;

import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.app.PendingIntent;
// Cordova-required packages
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.apache.cordova.LOG;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class EsimPlugin extends CordovaPlugin {
    protected static final String LOG_TAG = "eSIM";
    private static final String HAS_ESIM_ENABLED = "hasEsimEnabled";
    private static final String ACTION_DOWNLOAD_SUBSCRIPTION = "download_subscription";
    private CallbackContext callback;

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        callback = callbackContext;
        try {
            if (HAS_ESIM_ENABLED.equals(action)) {
                LOG.i(LOG_TAG, "checking eSIM support");
                hasEsimEnabled();
                return true;
            }else if (ACTION_DOWNLOAD_SUBSCRIPTION.equals(action)) {   
                LOG.i(LOG_TAG, "install eSIM");     
                installEsim(args, callbackContext);
                return true;
            }else{
                return false;
            }
        } catch (Exception e) {
            LOG.e(LOG_TAG, "Error execute "  + e.getMessage());
            callback.sendPluginResult(new PluginResult(Status.ERROR));
            return false;
        }
    }

    private void hasEsimEnabled() {
        Context context = this.cordova.getActivity().getApplicationContext();
        EuiccManager mgr = (EuiccManager) context.getSystemService(Context.EUICC_SERVICE);
        boolean result = mgr.isEnabled();
        callback.sendPluginResult(new PluginResult(Status.OK, result));
    }
    private void installEsim(JSONArray args, CallbackContext callbackContext) throws JSONException{
        Context mainContext = this.cordova.getActivity().getApplicationContext();
        // Register receiver.
        String LPA_DECLARED_PERMISSION = "com.your.company.lpa.permission.BROADCAST";
        String address = args.getString(0);
        String matchingID = args.getString(1);
        String activationCode = "1$" + address + "$" + matchingID;
        EuiccManager mgr = (EuiccManager) mainContext.getSystemService(Context.EUICC_SERVICE);
        LOG.i(LOG_TAG, "activationCode = " + activationCode + "\n LPA_DECLARED_PERMISSION: " + LPA_DECLARED_PERMISSION);
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
                            LOG.e(LOG_TAG, "Error startResolutionActivity "  + e.getMessage());        
                            callbackContext.error(e.getMessage());    
                            callbackContext.sendPluginResult(new PluginResult(Status.ERROR));                 
                        }
                    } else if (resultCode == EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_ERROR) {
                        // Embedded Subscription Error
                        LOG.i(LOG_TAG, "EMBEDDED_SUBSCRIPTION_RESULT_ERROR - Can't add an Esim subscription");        
                        callbackContext.error("EMBEDDED_SUBSCRIPTION_RESULT_ERROR - Can't add an Esim subscription");  
                    } else {
                        LOG.i(LOG_TAG, "Can't add an Esim subscription due to unknown error");        
                        callbackContext.error("Can't add an Esim subscription due to unknown error"); 
                    } 
                    Intent resultIntent = intent;
                }
            };
            mainContext.registerReceiver(receiver, new IntentFilter(ACTION_DOWNLOAD_SUBSCRIPTION), null, null);

            
            // Download subscription asynchronously.
            DownloadableSubscription sub = DownloadableSubscription.forActivationCode(activationCode);
            PendingIntent callbackIntent = PendingIntent.getBroadcast(
                mainContext,
                0,
                new Intent(ACTION_DOWNLOAD_SUBSCRIPTION),
                PendingIntent.FLAG_UPDATE_CURRENT |
                    PendingIntent.FLAG_MUTABLE);
        
            mgr.downloadSubscription(sub, true, callbackIntent);            
            callbackContext.sendPluginResult(new PluginResult(Status.OK, "success"));
        }catch (Exception e) {
            LOG.e(LOG_TAG, "Error install eSIM "  + e.getMessage());
            callbackContext.error(e.getMessage());
            callbackContext.sendPluginResult(new PluginResult(Status.ERROR));
        }
    }       
}
