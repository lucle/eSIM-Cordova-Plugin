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
    
    private static final String HAS_ESIM_ENABLED = "hasEsimEnabled";
    private static final String INSTALL_ESIM = "installEsim";
    private static final String ACTION_DOWNLOAD_SUBSCRIPTION = "download_subscription";
    private Context mainContext;
    private EuiccManager mgr;
    
    private static final String LPA_DECLARED_PERMISSION = "com.starhub.aduat.torpedo.lpa.permission.BROADCAST";
    String address;
    String matchingID;
    String activationCode ;

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        mainContext = this.cordova.getActivity().getApplicationContext();
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
        TelephonyManager telephonyManager  = (TelephonyManager) mainContext.getSystemService(Context.TELEPHONY_SERVICE);
        boolean isCarrier = telephonyManager.hasCarrierPrivileges();
        return isCarrier;
    }

    private void hasEsimEnabled(CallbackContext callbackContext) {
        initMgr();
        boolean result = mgr.isEnabled();
        callbackContext.sendPluginResult(new PluginResult(Status.OK, result));
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
                        callbackContext.error("Can't setup eSim due to wrong Intent:" + intent.getAction() + " instead of " + ACTION_DOWNLOAD_SUBSCRIPTION); 
                        return;
                    }
                    
                    int resultCode = getResultCode();
                    int detailedCode = intent.getIntExtra(EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_DETAILED_CODE, 0);
                    int operationCode = intent.getIntExtra(EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_OPERATION_CODE,-1);
                    int errorCode = intent.getIntExtra(EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_ERROR_CODE,-1);
                    String smdxSubjectCode = intent.getStringExtra(EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_SMDX_SUBJECT_CODE);
                    String smdxReasonCode = intent.getStringExtra(EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_SMDX_REASON_CODE);

                    if (resultCode == EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_RESOLVABLE_ERROR && mgr != null) {                      
                        // Resolvable error, attempt to resolve it by a user action
                        int resolutionRequestCode = 0;
                        PendingIntent callbackIntent = PendingIntent.getBroadcast(
                            mainContext, 
                            resolutionRequestCode /* requestCode */, 
                            new Intent(ACTION_DOWNLOAD_SUBSCRIPTION), 
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
                        try{
                            mgr.startResolutionActivity(cordova.getActivity(), resolutionRequestCode /* requestCode */, intent, callbackIntent);
                        } catch (Exception e) { 
                            callbackContext.error("Error startResolutionActivity - Can't add an Esim subscription: " + e.getLocalizedMessage() + " detailedCode=" + detailedCode + 
                                " operationCode=" + operationCode + " errorCode=" + errorCode + " smdxSubjectCode=" + smdxSubjectCode + " smdxReasonCode=" + smdxReasonCode + " activationCode=" + activationCode);
                            callbackContext.sendPluginResult(new PluginResult(Status.ERROR));     
                        }                                               
                    } else if (resultCode == EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_OK) {  
                        callbackContext.sendPluginResult(new PluginResult(Status.OK, true));
                    } else if (resultCode == EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_ERROR) {
                        // Embedded Subscription Error     
                        callbackContext.error("EMBEDDED_SUBSCRIPTION_RESULT_ERROR - Can't add an Esim subscription: detailedCode=" + detailedCode + 
                                " operationCode=" + operationCode + " errorCode=" + errorCode + " smdxSubjectCode=" + smdxSubjectCode + " smdxReasonCode=" + smdxReasonCode + " activationCode=" + activationCode);  
                    } else { 
                        callbackContext.error("Can't add an Esim subscription due to unknown error, resultCode is:" + String.valueOf(resultCode)); 
                    }
                }
            };
            mainContext.registerReceiver(
                receiver, 
                new IntentFilter(ACTION_DOWNLOAD_SUBSCRIPTION), 
                LPA_DECLARED_PERMISSION /* broadcastPermission*/, 
                null /* handler */
            );
            
            address = args.getString(0);
            matchingID = ReFormatString(args.getString(1), 4);
            activationCode = "1$" + address + "$" + matchingID;

            // Download subscription asynchronously.
            DownloadableSubscription sub = DownloadableSubscription.forActivationCode(activationCode /* encodedActivationCode*/);
        
            PendingIntent callbackIntent = PendingIntent.getBroadcast(
                mainContext,
                0 /* requestCode */,
                new Intent(ACTION_DOWNLOAD_SUBSCRIPTION),
                PendingIntent.FLAG_UPDATE_CURRENT |
                    PendingIntent.FLAG_MUTABLE);
        
            mgr.downloadSubscription(sub, true /* switchAfterDownload */, callbackIntent);  
            //callbackContext.sendPluginResult(new PluginResult(Status.OK, true));
        }catch (Exception e) {
            callbackContext.error("Error install eSIM "  + e.getMessage());
            callbackContext.sendPluginResult(new PluginResult(Status.ERROR));
        }
    }
    
    public String ReFormatString(String S, int K){
        int len = S.length();
        int cnt = 0;
        int x = 0;
    
        // Move the characters to the
        // back of the string.
        for (int i = len - 1; i >= 0; i--) {
        if (S.charAt(i) == '-') {
            x++;
        }
        else {
            S = S.substring(0, i + x)
            + Character.toUpperCase(S.charAt(i))
            + S.substring(i + x + 1);
        }
        }
    
        // Calculate total number of
        // alphanumeric characters so
        // as to get the number of dashes
        // in the final string.
        int slen = len - x;
        int step = (int)(slen / K);
    
        // Remove x characters from the
        // start of the string
    
        S = reverseS(S);
        int val = x;
        while (val > 0) {
        S = S.substring(0, S.length() - 1);
        val--;
        }
    
        // Push the empty spaces in
        // the string (slen+step) to get
        // the final string length
    
        int temp = step;
        while (temp > 0) {
        S += " ";
        temp--;
        }
        S = reverseS(S);
    
        len = S.length();
    
        // Using simple mathematics
        // to push the elements
        // in the string at the correct place.
    
        int i = slen, j = step, f = 0;
        while (j < len) {
    
        // At every step calculate the
        // number of dashes that would be
        // present before the character
        step = (int)(i / K);
        if (f == 1)
            step--;
        int rem = i % K;
    
        // If the remainder is zero it
        // implies that the character is a dash.
    
        if (rem == 0 && f == 0) {
            S = S.substring(0, j - step) + "-"
            + S.substring(j - step + 1);
            f = 1;
            continue;
        }
        S = S.substring(0, j - step) + S.charAt(j)
            + S.substring(j - step + 1);
        i--;
        j++;
        f = 0;
        }
        // Remove all the dashes that would have
        // accumulated in the beginning of the string.
    
        len = S.length();
        S = reverseS(S);
        for (int m = len - 1; m >= 0; m--) {
        if (S.charAt(m) != '-') {
            break;
        }
        if (S.charAt(m) == '-')
            S = S.substring(0, S.length() - 1);
        }
        S = reverseS(S);
    
        return S;
    }
  
    public String reverseS(String str)
    {
        String nstr = "";
        for (int i = 0; i < str.length(); i++) {
            char ch
            = str.charAt(i); // extracts each character
            nstr
            = ch + nstr; // adds each character in
            // front of the existing string
        }
        return nstr;
    }
}
