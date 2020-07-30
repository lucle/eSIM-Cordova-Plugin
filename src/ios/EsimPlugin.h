#import <Cordova/CDV.h>

@interface EsimPlugin : CDVPlugin

- (void)hasEsimEnabled:(CDVInvokedUrlCommand*)command;
- (BOOL)hasEsimInstalled:(CDVInvokedUrlCommand*)command;
@end
