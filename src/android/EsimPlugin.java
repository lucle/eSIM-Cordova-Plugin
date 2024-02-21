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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class EsimPlugin extends CordovaPlugin{
    // CONSTANTS //
    private static final String DOWNLOAD_ACTION = "download_subscription"
    private static final String START_RESOLUTION_ACTION = "start_resolution_action"
    private static final String BROADCAST_PERMISSION = "com.starhub.aduat.torpedo.lpa.permission.BROADCAST";

    private var manager: EuiccManager? = null

    // eSIM Constants //
    private static final String HAS_ESIM_ENABLED = "hasEsimEnabled";
    private static final String INSTALL_ESIM = "installEsim";
	private static final String INSTALL_ESIM_NEW = "installEsimNew";
    // FUNCTIONS //

    // Initiate Manager
    private void initMgr() {
        if (manager == null) {
            manager = (EuiccManager) mainContext.getSystemService(Context.EUICC_SERVICE);
        }
    }
    // e-Sim compatibility check
    private void isSupportEsim(CallbackContext callbackContext) {
        initMgr();
        boolean result = manager.isEnabled();
        callbackContext.sendPluginResult(new PluginResult(Status.OK, result));
    }
    // 
    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if (HAS_ESIM_ENABLED.equals(action)) {
            hasEsimEnabled(callbackContext);
        }else if (INSTALL_ESIM.equals(action)) {   
            installEsim(args, callbackContext);
		}else if (INSTALL_ESIM_NEW.equals(action)) {   
            installEsimNew(args, callbackContext);
        }else {
            return false;
        }    
        return true;
    }
        /**
     * Register the broadcast receivers
     */
    @Override
    protected void onStart()
    {
        super.onStart()
        registerReceiver(
            eSimBroadcastReceiver,
            IntentFilter(DOWNLOAD_ACTION),
            BROADCAST_PERMISSION,
            null
        )
        registerReceiver(
            resolutionReceiver,
            IntentFilter(START_RESOLUTION_ACTION),
            BROADCAST_PERMISSION,
            null
        )
    };

    /**
     * Un-Register the broadcast receivers
     */
    @Override
    protected void onStop()
    {
        super.onStop()
        unregisterReceiver(eSimBroadcastReceiver)
        unregisterReceiver(resolutionReceiver)
    };

    private boolean checkCarrierPrivileges() {
        TelephonyManager telephonyManager  = (TelephonyManager) mainContext.getSystemService(Context.TELEPHONY_SERVICE);
        boolean isCarrier = telephonyManager.hasCarrierPrivileges();
        return isCarrier;

    }

	private void installEsimNew(JSONArray args, CallbackContext callbackContext)  
    {
        try {
            BroadcastReceiver receiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (!action.equals(intent.getAction())) {
                        callbackContext.error("Can't setup eSim due to wrong Intent:" + intent.getAction() + " instead of " + ACTION_DOWNLOAD_SUBSCRIPTION); 
                        return;
                    }

                    int resultCode = getResultCode();
                    int detailedCode = intent.getIntExtra(EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_DETAILED_CODE, 0);
                    int operationCode = intent.getIntExtra(EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_OPERATION_CODE,0);
                    int errorCode = intent.getIntExtra(EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_ERROR_CODE,0);
                    String smdxSubjectCode = intent.getStringExtra(EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_SMDX_SUBJECT_CODE);
                    String smdxReasonCode = intent.getStringExtra(EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_SMDX_REASON_CODE);
                    String packageName = mainContext.getPackageName();
                    boolean hasCarrierPrivileges = checkCarrierPrivileges();

                    // If the result code is a resolvable error, call startResolutionActivity
                    if (resultCode == EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_RESOLVABLE_ERROR) {
                        PendingIntent callbackIntent = PendingIntent.getBroadcast(
                            getContext(), 0 /* requestCode */, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
                        try {
                            manager.startResolutionActivity(
                            activity,
                            0 /* requestCode */,
                            intent,
                            callbackIntent);
                        }
                        catch (Exception e) { 
                            callbackContext.error("Error startResolutionActivity - Can't add an Esim subscription: " + e.getLocalizedMessage() + " detailedCode=" + detailedCode + 
                                " operationCode=" + operationCode + " errorCode=" + errorCode + " smdxSubjectCode=" + smdxSubjectCode + " smdxReasonCode=" + smdxReasonCode + " activationCode=" + activationCode + 
                                " hasCarrierPrivileges:" + hasCarrierPrivileges + " package Name = " + packageName);
                            callbackContext.sendPluginResult(new PluginResult(Status.ERROR));     
                        }

                    }
                    else if (resultCode == EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_ERROR) {
                        // Embedded Subscription Error     
                        callbackContext.error("Error Embedded Subscription Error - Can't add an Esim subscription: " + " detailedCode=" + detailedCode + 
                        " operationCode=" + operationCode + " errorCode=" + errorCode + " smdxSubjectCode=" + smdxSubjectCode + " smdxReasonCode=" + smdxReasonCode + " activationCode=" + activationCode + 
                        " hasCarrierPrivileges:" + hasCarrierPrivileges + " package Name = " + packageName);
                        callbackContext.error("Can't add an Esim subscription due to unknown error, resultCode is:" + String.valueOf(resultCode) + " hasCarrierPrivileges" + hasCarrierPrivileges);
                    }

                    resultIntent = intent;
                }
            };
            context.registerReceiver(receiver,
                    new IntentFilter(DOWNLOAD_ACTION),
                    BROADCAST_PERMISSION /* broadcastPermission*/,
                    null /* handler */);
            // Download subscription asynchronously.
            DownloadableSubscription sub = DownloadableSubscription
            .forActivationCode(code /* encodedActivationCode*/);
            Intent intent = new Intent(action).setPackage(context.getPackageName());
            PendingIntent callbackIntent = PendingIntent.getBroadcast(
            getContext(), 0 /* requestCode */, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
            manager.downloadSubscription(sub, true /* switchAfterDownload */,
            callbackIntent);
        }catch (Exception e) {
            callbackContext.error("Error install eSIM "  + e.getMessage());
            callbackContext.sendPluginResult(new PluginResult(Status.ERROR));
        }
    }

}
