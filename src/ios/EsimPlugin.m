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

- (BOOL)installEsim:(CDVInvokedUrlCommand*)command
{
        BOOL result = NO;
        CTCellularPlanProvisioningRequest *ctpr = [[CTCellularPlanProvisioningRequest alloc] init];
        ctpr.address = [command argumentAtIndex:0 withDefault:nil]; 
        ctpr.matchingID = [command argumentAtIndex:1 withDefault:nil]; 
        ctpr.iccid = [command argumentAtIndex:2 withDefault:nil]; 
        ctpr.confirmationCode = [command argumentAtIndex:3 withDefault:nil]; 

        if (@available(iOS 12, *)) {
            CTCellularPlanProvisioning *ctcp = [[CTCellularPlanProvisioning alloc] init];
            [ctcp addPlanWith:ctpr completionHandler:^(CTCellularPlanProvisioningAddPlanResult result) {
                switch (result) {
                    case CTCellularPlanProvisioningAddPlanResultSuccess:
                        result = YES; //eSIM installed successfully
                        break;
                    default:
                        result = NO; //something went wrong
                        break;
                }
            }];
        }
        return result;
}

@end
