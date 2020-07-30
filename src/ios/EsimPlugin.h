#import <Cordova/CDV.h>

@interface EsimPlugin : CDVPlugin

- (void)hasEsimEnabled:(CDVInvokedUrlCommand*)command;
- (BOOL)installEsim:(CDVInvokedUrlCommand*)command;
@end
