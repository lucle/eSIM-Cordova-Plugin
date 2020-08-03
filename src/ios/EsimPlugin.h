#import <Cordova/CDV.h>

@interface EsimPlugin : CDVPlugin

- (void)hasEsimEnabled:(CDVInvokedUrlCommand*)command;
- (void)installEsim:(CDVInvokedUrlCommand*)command;
@end
