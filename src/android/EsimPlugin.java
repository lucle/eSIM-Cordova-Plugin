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
    private static final String INSTALL_ESIM = "installEsim";
    private static final String ACTION_DOWNLOAD_SUBSCRIPTION = "download_subscription";
    private Context mainContext;
    private EuiccManager mgr;
    
    private static final String LPA_DECLARED_PERMISSION = "com.dreamcloud.lpa.permission.BROADCAST";
    String address;
    String matchingID;
    String activationCode ;

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        mainContext = cordova.getContext();
        LOG.i(LOG_TAG, "initialize()");
     }

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if (HAS_ESIM_ENABLED.equals(action)) {
            LOG.i(LOG_TAG, "checking eSIM support");
            hasEsimEnabled(callbackContext);
        }else if (INSTALL_ESIM.equals(action)) {   
            LOG.i(LOG_TAG, "install eSIM");     
            installEsim(args, callbackContext);
        }else {
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

    private void installEsim(JSONArray args, CallbackContext callbackContext) {         
        try{
            initMgr(); 
            address = args.getString(0);
            matchingID = args.getString(1);
            activationCode = "1$" + address + "$" + matchingID;
            LOG.i(LOG_TAG, "activationCode = " + activationCode + "\n LPA_DECLARED_PERMISSION: " + LPA_DECLARED_PERMISSION);
            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (!ACTION_DOWNLOAD_SUBSCRIPTION.equals(intent.getAction())) {
                        return;
                    }
                    int resultCode = getResultCode();
                    // If the result code is a resolvable error, call startResolutionActivity
                    if (resultCode == EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_RESOLVABLE_ERROR && mgr != null) {                      
                        int resolutionRequestCode = resultCode;
                        PendingIntent callbackIntent = PendingIntent.getBroadcast(cordova.getContext(), resolutionRequestCode, 
                            intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
                        try{
                            mgr.startResolutionActivity(cordova.getActivity(), resolutionRequestCode, intent, callbackIntent);
                        } catch (Exception e) {  
                            LOG.e(LOG_TAG, "Error startResolutionActivity "  + e.getMessage());        
                            callbackContext.error("Error startResolutionActivity "  + e.getMessage());    
                            callbackContext.sendPluginResult(new PluginResult(Status.ERROR));                 
                        }
                    } else if (resultCode == EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_OK) {
                        LOG.i(LOG_TAG, "EMBEDDED_SUBSCRIPTION_RESULT_OK " + String.valueOf(resultCode));        
                        callbackContext.error("EMBEDDED_SUBSCRIPTION_RESULT_OK " + String.valueOf(resultCode)); 
                    } else if (resultCode == EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_ERROR) {
                        // Embedded Subscription Error
                        int detailedCode = intent.getIntExtra(EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_DETAILED_CODE, 0);

                        LOG.i(LOG_TAG, "EMBEDDED_SUBSCRIPTION_RESULT_ERROR - Can't add an Esim subscription" + detailedCode);        
                        callbackContext.error("EMBEDDED_SUBSCRIPTION_RESULT_ERROR - Can't add an Esim subscription " + detailedCode);  
                    } else {
                        LOG.i(LOG_TAG, "Can't add an Esim subscription due to unknown error, resultCode is:" + String.valueOf(resultCode));        
                        callbackContext.error("Can't add an Esim subscription due to unknown error, resultCode is:" + String.valueOf(resultCode)); 
                    } 
                    Intent resultIntent = intent;
                }
            };
            mainContext.registerReceiver(receiver, new IntentFilter(ACTION_DOWNLOAD_SUBSCRIPTION), null , null);
         
            // Download subscription asynchronously.
            DownloadableSubscription sub = DownloadableSubscription.forActivationCode(activationCode);
            Intent intent = new Intent(ACTION_DOWNLOAD_SUBSCRIPTION);
            PendingIntent callbackIntent = PendingIntent.getBroadcast(
                mainContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT |
                    PendingIntent.FLAG_MUTABLE);
        
            mgr.downloadSubscription(sub, true, callbackIntent);            
            //callbackContext.sendPluginResult(new PluginResult(Status.OK, "success"));
        }catch (Exception e) {
            LOG.e(LOG_TAG, "Error install eSIM "  + e.getMessage());
            callbackContext.error("Error install eSIM "  + e.getMessage());
            callbackContext.sendPluginResult(new PluginResult(Status.ERROR));
        }
    }       
}
