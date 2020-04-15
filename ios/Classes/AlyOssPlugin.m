#import <AliyunOSSiOS/OSSService.h>
#import "AlyOssPlugin.h"

NSObject<FlutterPluginRegistrar> *REGISTRAR;
FlutterMethodChannel *CHANNEL;
OSSClient *oss = nil;

@implementation AlyOssPlugin
+  (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
    CHANNEL = [FlutterMethodChannel
               methodChannelWithName:@"aly_oss"
               binaryMessenger:[registrar messenger]];
    AlyOssPlugin* instance = [[AlyOssPlugin alloc] init];
    [registrar addMethodCallDelegate:instance channel:CHANNEL];
}

- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {
    if ([@"upload" isEqualToString:call.method]) {
        [self upload:call result:result];
        
        return;
    } else if ([@"delete" isEqualToString:call.method]) {
        [self delete:call result:result];
        
        return;
    } else {
        result(FlutterMethodNotImplemented);
    }
}

- (void)init:(FlutterMethodCall*)call {
    NSString *accessKeyId = call.arguments[@"accessKeyId"];
    NSString *accessKeySecret = call.arguments[@"accessKeySecret"];
     NSString *securityToken = call.arguments[@"securityToken"];
    // NSString *expiration = call.arguments[@"expiration"];
    NSString *endpoint = call.arguments[@"endpoint"];
//    id<OSSCredentialProvider> credential = [[OSSPlainTextAKSKPairCredentialProvider alloc] initWithPlainTextAccessKey:accessKeyId    secretKey:accessKeySecret];
     id<OSSCredentialProvider> credential = [[OSSStsTokenCredentialProvider alloc] initWithAccessKeyId:accessKeyId secretKeyId:accessKeySecret securityToken:securityToken];
    
    oss = [[OSSClient alloc] initWithEndpoint:endpoint credentialProvider:credential];
}

- (void)upload:(FlutterMethodCall*)call result:(FlutterResult)result {
    if (oss == nil) {
        [self init: call];
    }
    
    NSString *instanceId = call.arguments[@"instanceId"];
    NSString *requestId = call.arguments[@"requestId"];
    NSString *bucket = call.arguments[@"bucket"];
    NSString *key = call.arguments[@"key"];
    NSString *file = call.arguments[@"file"];
    
    OSSPutObjectRequest *request = [OSSPutObjectRequest new];
    request.bucketName = bucket;
    request.objectKey = key;
    request.uploadingFileURL = [NSURL fileURLWithPath:file];
    request.uploadProgress = ^(int64_t bytesSent, int64_t totalByteSent, int64_t totalBytesExpectedToSend) {
        NSDictionary *arguments = @{
            @"instanceId":instanceId,
            @"requestId":requestId,
            @"bucket":bucket,
            @"key":key,
            @"currentSize":  [NSString stringWithFormat:@"%lld",totalByteSent],
            @"totalSize": [NSString stringWithFormat:@"%lld",totalBytesExpectedToSend]
        };
        [CHANNEL invokeMethod:@"onProgress" arguments:arguments];
    };
    
    OSSTask *task = [oss putObject:request];
    [task continueWithBlock:^id(OSSTask *task) {
        if (!task.error) {
            NSDictionary *arguments = @{
                @"success": @"true",
                @"instanceId":instanceId,
                @"requestId":requestId,
                @"bucket":bucket,
                @"key":key,
            };
            [CHANNEL invokeMethod:@"onUpload" arguments:arguments];
        } else {
            NSDictionary *arguments = @{
                @"success": @"false",
                @"instanceId":instanceId,
                @"requestId":requestId,
                @"bucket":bucket,
                @"key":key,
                @"message":task.error
            };
            [CHANNEL invokeMethod:@"onUpload" arguments:arguments];
        }
        return nil;
    }];
}

- (void)delete:(FlutterMethodCall*)call result:(FlutterResult)result {
    if (oss == nil) {
        [self init: call];
    }
    
    NSString *instanceId = call.arguments[@"instanceId"];
    NSString *requestId = call.arguments[@"requestId"];
    NSString *bucket = call.arguments[@"bucket"];
    NSString *key = call.arguments[@"key"];
    
    OSSDeleteObjectRequest *request = [OSSDeleteObjectRequest new];
    request.bucketName = bucket;
    request.objectKey = key;
    
    OSSTask *task = [oss deleteObject:request];
    
    [task continueWithBlock:^id(OSSTask *task) {
        return nil;
    }];
    
    [task waitUntilFinished];
    
    if (task.error) {
        result([FlutterError errorWithCode:@"SERVICE_EXCEPTION"
                                   message:@""
                                   details:nil]);
    } else {
        NSDictionary *arguments = @{
            @"instanceId": instanceId,
            @"requestId":requestId,
            @"bucket":bucket,
            @"key":key
        };
        
        result(arguments);
    }
}

@end

