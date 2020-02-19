package com.dreamcloud.cordova.plugin;

// The native android API
import android.telephony.euicc.EuiccManager;

// Cordova-required packages
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class EsimPlugin extends CordovaPlugin {

  private static final String HAS_ESIM_ENABLED = "hasEsimEnabled";
  private CallbackContext callback;

  @Override
  public boolean execute(String action, JSONArray args,
    final CallbackContext callbackContext) throws JSONException {

    callback = callbackContext;

    if (HAS_ESIM_ENABLED.equals(action)) {
      hasEsimEnabled();
      return true;
    } else {
      return false;
    }
  }

  private void hasEsimEnabled() {
    EuiccManager mgr = (EuiccManager) context.getSystemService(Context.EUICC_SERVICE);
    boolean result = mgr.isEnabled();
    callbackContext.sendPluginResult(new PluginResult(Status.OK, result));
  }
}