#import "EsimPlugin.h"
#import <Cordova/CDV.h>
#import <Foundation/Foundation.h>
#import <CoreTelephony/CTCellularPlanProvisioning.h>

@implementation EsimPlugin

- (void)hasEsimEnabled:(CDVInvokedUrlCommand*)command
{
    if (@available(iOS 12.0, *)) {
        CTCellularPlanProvisioning *planProvisioning = [[CTCellularPlanProvisioning alloc] init];
        BOOL isEsimEnabled = [planProvisioning supportsCellularPlan];
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:isEsimEnabled];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    } else {
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
}

@end
