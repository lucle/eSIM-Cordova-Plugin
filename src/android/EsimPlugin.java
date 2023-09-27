package com.dreamcloud;
// The native android API
import android.telephony.euicc.EuiccManager;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
// Cordova-required packages
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
public class EsimPlugin extends CordovaPlugin {
    private static final String HAS_ESIM_ENABLED = "hasEsimEnabled";
    Context context;
    EuiccManager mgr;
    private CallbackContext callback;
     // at the initialize function, we can configure the tools we want to use later, like the sensors
     @Override
     public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        context = this.cordova.getActivity().getApplicationContext();
        mgr = (EuiccManager) context.getSystemService(Context.EUICC_SERVICE);
     }
    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        callback = callbackContext;
        if (HAS_ESIM_ENABLED.equals(action)) {
            hasEsimEnabled();
            return true;
        } else {
            return false;
        }
    }
    private void hasEsimEnabled() {
        boolean result = mgr.isEnabled();
        callback.sendPluginResult(new PluginResult(Status.OK, result));
    }
    private void installEsim(String SDMPAddress, String activationCode) {
        // Register receiver.
        String action = "download_subscription";
        String LPA_DECLARED_PERMISSION = SDMPAddress;
        BroadcastReceiver receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (!action.equals(intent.getAction())) {
                            return;
                        }
                        String resultCode = getResultCode();
                        String detailedCode = intent.getIntExtra(EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_DETAILED_CODE, 0);

                        // If the result code is a resolvable error, call startResolutionActivity
                        if (resultCode == EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_RESOLVABLE_ERROR) {
                            PendingIntent callbackIntent = PendingIntent.getBroadcast(getContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
                            mgr.startResolutionActivity(activity, 0, intent, callbackIntent);
                        }
                        String resultIntent = intent;
                    }
                };
        context.registerReceiver(receiver, new IntentFilter(action), LPA_DECLARED_PERMISSION, null);

        // Download subscription asynchronously.
        DownloadableSubscription sub = DownloadableSubscription.forActivationCode(activationCode);
        Intent intent = new Intent(action);
        PendingIntent callbackIntent = PendingIntent.getBroadcast(getContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        mgr.downloadSubscription(sub, true, callbackIntent);
        callback.sendPluginResult(new PluginResult(Status.OK));
    }       
}
