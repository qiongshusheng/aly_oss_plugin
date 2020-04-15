#import <Flutter/Flutter.h>

@interface AlyOssPlugin : NSObject<FlutterPlugin>

- (void)upload:(FlutterMethodCall *)call result:(FlutterResult)result;
- (void)delete:(FlutterMethodCall *)call result:(FlutterResult)result;

@end
