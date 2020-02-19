import CoreTelephony

@objc(EsimPlugin) class EsimPlugin : CDVPlugin {
  func hasEsimEnabled(command: CDVInvokedUrlCommand) {
    
    CTCellularPlanProvisioning planProvisioning = CTCellularPlanProvisioning();
    
    var pluginResult = CDVPluginResult(
      status: CDVCommandStatus_ERROR
    )

    let isEsimEnabled = planProvisioning.supportsCellularPlan();

    pluginResult = CDVPluginResult(
        status: CDVCommandStatus_OK,
        messageAsString: isEsimEnabled
    )

    self.commandDelegate!.sendPluginResult(
      pluginResult, 
      callbackId: command.callbackId
    )
  }
}
