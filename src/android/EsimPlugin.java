package com.dreamcloud;
// The native android API
import android.telephony.euicc.EuiccManager;
import android.telephony.euicc.DownloadableSubscription;
import android.telephony.TelephonyManager;

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
        mainContext = this.cordova.getActivity().getApplicationContext();
        LOG.i(LOG_TAG, "initialize()");
     }

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if (HAS_ESIM_ENABLED.equals(action)) {
            hasEsimEnabled(callbackContext);
        }else if (INSTALL_ESIM.equals(action)) {   
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

    private boolean checkCarrierPrivileges() {
        TelephonyManager telManager = (TelephonyManager) mainContext.getSystemService(Context.TELEPHONY_SERVICE);
            return telManager.hasCarrierPrivileges();
    }

    private void hasEsimEnabled(CallbackContext callbackContext) {
        initMgr();
        boolean result = mgr.isEnabled();
        callbackContext.sendPluginResult(new PluginResult(Status.OK, result));
    }

    private void handleResolvableError(CallbackContext callbackContext, Intent intent) {
        // Resolvable error, attempt to resolve it by a user action
        int resolutionRequestCode = 3;
        PendingIntent callbackIntent = PendingIntent.getBroadcast(
            cordova.getContext(), 
            resolutionRequestCode, 
            new Intent(ACTION_DOWNLOAD_SUBSCRIPTION), 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        try{
            mgr.startResolutionActivity(this.cordova.getActivity(), resolutionRequestCode, intent, callbackIntent);
        } catch (Exception e) {          
            LOG.d(LOG_TAG, "Error startResolutionActivity " + e.getMessage());
            callbackContext.error("Error startResolutionActivity "  + e.getMessage());    
            callbackContext.sendPluginResult(new PluginResult(Status.ERROR));                 
        }
    }

    private void installEsim(JSONArray args, CallbackContext callbackContext) {         
        try{
            initMgr(); 
            
            // if (!checkCarrierPrivileges()) {
            //     callbackContext.error("No carrier privileges detected");
            //     return;
            // }
            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (!ACTION_DOWNLOAD_SUBSCRIPTION.equals(intent.getAction())) {       
                        LOG.d(LOG_TAG, "Can't setup eSim due to wrong Intent:" + intent.getAction() + " instead of " + ACTION_DOWNLOAD_SUBSCRIPTION); 
                        callbackContext.error("Can't setup eSim due to wrong Intent:" + intent.getAction() + " instead of " + ACTION_DOWNLOAD_SUBSCRIPTION); 
                        return;
                    }
                    int resultCode = getResultCode();
                    // If the result code is a resolvable error, call startResolutionActivity
                    if (resultCode == EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_RESOLVABLE_ERROR && mgr != null) {                      
                        // Resolvable error, attempt to resolve it by a user action
                        int resolutionRequestCode = 3;
                        PendingIntent callbackIntent = PendingIntent.getBroadcast(
                            mainContext, 
                            resolutionRequestCode, 
                            new Intent(ACTION_DOWNLOAD_SUBSCRIPTION), 
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
                        try{
                            mgr.startResolutionActivity(this.cordova.getActivity(), resolutionRequestCode, intent, callbackIntent);
                        } catch (Exception e) {          
                            LOG.d(LOG_TAG, "Error startResolutionActivity " + e.getMessage());
                            callbackContext.error("Error startResolutionActivity "  + e.getMessage());    
                            callbackContext.sendPluginResult(new PluginResult(Status.ERROR));                 
                        }
                    } else if (resultCode == EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_OK) {
                        LOG.i(LOG_TAG, "EMBEDDED_SUBSCRIPTION_RESULT_OK " + String.valueOf(resultCode));     
                        callbackContext.sendPluginResult(new PluginResult(Status.OK, true));
                    } else if (resultCode == EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_ERROR) {
                        // Embedded Subscription Error     
                        LOG.i(LOG_TAG, "EMBEDDED_SUBSCRIPTION_RESULT_ERROR - Can't add an Esim subscription");
                        callbackContext.error("EMBEDDED_SUBSCRIPTION_RESULT_ERROR - Can't add an Esim subscription" );  
                    } else {   
                        LOG.i(LOG_TAG, "Can't add an Esim subscription due to unknown error, resultCode is:" + String.valueOf(resultCode));   
                        callbackContext.error("Can't add an Esim subscription due to unknown error, resultCode is:" + String.valueOf(resultCode)); 
                    }
                }
            };
            mainContext.registerReceiver(
                receiver, 
                new IntentFilter(ACTION_DOWNLOAD_SUBSCRIPTION), 
                LPA_DECLARED_PERMISSION , 
                null
            );
            
            address = args.getString(0);
            matchingID = args.getString(1);
            activationCode = "1$" + address + "$" + matchingID;
            LOG.i(LOG_TAG, "activationCode = " + activationCode + "\n LPA_DECLARED_PERMISSION: " + LPA_DECLARED_PERMISSION);
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
            LOG.d(LOG_TAG, "Error install eSIM " + e.getMessage());
            callbackContext.error("Error install eSIM "  + e.getMessage());
            callbackContext.sendPluginResult(new PluginResult(Status.ERROR));
        }
    }       
}
