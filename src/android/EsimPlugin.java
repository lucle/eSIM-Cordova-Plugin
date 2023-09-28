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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class EsimPlugin extends CordovaPlugin {
    private static final String HAS_ESIM_ENABLED = "hasEsimEnabled";
    private static final String ACTION_SWITCH_TO_SUBSCRIPTION  = "download_subscription";
    Context mainContext;
    EuiccManager mgr;
    private CallbackContext callback;
     // at the initialize function, we can configure the tools we want to use later, like the sensors
     @Override
     public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        mainContext = this.cordova.getActivity().getApplicationContext();
        mgr = (EuiccManager) mainContext.getSystemService(Context.EUICC_SERVICE);
     }
    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        callback = callbackContext;
        if (HAS_ESIM_ENABLED.equals(action)) {
            hasEsimEnabled();
            return true;
        }else if (ACTION_SWITCH_TO_SUBSCRIPTION.equals(action)) {
            String address = args.getString(0);
            String activationCode = args.getString(1);
            String iccid = args.getString(2);
            String confirmationCode = args.getString(3);
            installEsim(address, activationCode, iccid, confirmationCode);
        } else {
            return false;
        }
    }
    private void hasEsimEnabled() {
        boolean result = mgr.isEnabled();
        callback.sendPluginResult(new PluginResult(Status.OK, result));
    }
    private void installEsim(String address, String activationCode, String iccid, String confirmationCode) throws JSONException{
        // Register receiver.
        String LPA_DECLARED_PERMISSION = address;
        BroadcastReceiver receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (!ACTION_SWITCH_TO_SUBSCRIPTION.equals(intent.getAction())) {
                            return;
                        }
                        int resultCode = getResultCode();
                        int detailedCode = intent.getIntExtra(EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_DETAILED_CODE, 0);

                        // If the result code is a resolvable error, call startResolutionActivity
                        if (resultCode == EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_RESOLVABLE_ERROR) {
                            try {
                                PendingIntent callbackIntent = PendingIntent.getBroadcast(mainContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
                                mgr.startResolutionActivity(cordova.getActivity(), 0, intent, callbackIntent);
                            } catch (Exception e) {                               
                            }
                        }
                        Intent resultIntent = intent;
                    }
                };
        mainContext.registerReceiver(receiver, new IntentFilter(ACTION_SWITCH_TO_SUBSCRIPTION), LPA_DECLARED_PERMISSION, null);

        // Download subscription asynchronously.
        DownloadableSubscription sub = DownloadableSubscription.forActivationCode(activationCode);
        Intent intent = new Intent(ACTION_SWITCH_TO_SUBSCRIPTION);
        PendingIntent callbackIntent = PendingIntent.getBroadcast(mainContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        mgr.downloadSubscription(sub, true, callbackIntent);
        callback.sendPluginResult(new PluginResult(Status.OK, "success"));
    }       
}
