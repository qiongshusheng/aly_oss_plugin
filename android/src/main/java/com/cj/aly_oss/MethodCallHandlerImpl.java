package com.cj.aly_oss;

import com.alibaba.sdk.android.oss.ClientConfiguration;
import com.alibaba.sdk.android.oss.ClientException;
import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.OSSClient;
import com.alibaba.sdk.android.oss.ServiceException;
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback;
import com.alibaba.sdk.android.oss.callback.OSSProgressCallback;
import com.alibaba.sdk.android.oss.common.auth.OSSAuthCredentialsProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSPlainTextAKSKCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSStsTokenCredentialProvider;
import com.alibaba.sdk.android.oss.model.DeleteObjectRequest;
import com.alibaba.sdk.android.oss.model.PutObjectRequest;
import com.alibaba.sdk.android.oss.model.PutObjectResult;
import com.google.common.collect.Maps;

import java.util.Map;
import io.flutter.Log;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;

public class MethodCallHandlerImpl implements MethodChannel.MethodCallHandler {
    private final MethodChannel channel;
    private final PluginRegistry.Registrar registrar;
    private OSS oss;

    MethodCallHandlerImpl(MethodChannel channel, PluginRegistry.Registrar registrar) {
        this.channel = channel;
        this.registrar = registrar;
    }

    @Override
    public void onMethodCall(MethodCall methodCall, MethodChannel.Result result) {
        switch (methodCall.method) {
            case "upload":
                upload(methodCall, result);
                break;
            case "delete":
                delete(methodCall, result);
                break;
            default:
                result.notImplemented();
        }
    }

    private void init(MethodCall call) {
        final String accessKeyId = call.argument("accessKeyId");
        final String accessKeySecret = call.argument("accessKeySecret");
        final String securityToken = call.argument("securityToken");
//        final String expiration = call.argument("expiration");
        final String endpoint = call.argument("endpoint");
        Log.i("init", "accessKeyId=" + accessKeyId + ", accessKeySecret=" + accessKeySecret + ", securityToken=" + securityToken + ", endpoint=" + endpoint);
        final OSSCredentialProvider credentialProvider = new OSSStsTokenCredentialProvider(accessKeyId, accessKeySecret, securityToken);
        final ClientConfiguration conf = new ClientConfiguration();
        conf.setConnectionTimeout(15 * 1000); // 连接超时时间，默认15秒
        conf.setSocketTimeout(15 * 1000); // Socket超时时间，默认15秒
        conf.setMaxConcurrentRequest(5); // 最大并发请求数，默认5个
        conf.setMaxErrorRetry(2); // 失败后最大重试次数，默认2次

        oss = new OSSClient(registrar.context(), endpoint, credentialProvider, conf);
    }

    private void upload(MethodCall call, MethodChannel.Result result) {
        if (oss == null) {
            init(call);
        }
        final String instanceId = call.argument("instanceId");
        final String requestId = call.argument("requestId");
        final String bucket = call.argument("bucket");
        final String key = call.argument("key");
        final String file = call.argument("file");

        Log.i("upload", "instanceId=" + instanceId + ", bucket=" + bucket + ", key=" + key + ", file=" + file);
        PutObjectRequest request = new PutObjectRequest(bucket, key, file);
        request.setProgressCallback(new OSSProgressCallback<PutObjectRequest>() {
            @Override
            public void onProgress(PutObjectRequest request, long currentSize, long totalSize) {
                final Map<String, String> arguments = Maps.newHashMap();
                arguments.put("instanceId", instanceId);
                arguments.put("requestId", requestId);
                arguments.put("bucket", bucket);
                arguments.put("key", key);
                arguments.put("currentSize", String.valueOf(currentSize));
                arguments.put("totalSize", String.valueOf(totalSize));
                invokeMethod("onProgress", arguments);
            }
        });

        oss.asyncPutObject(request, new OSSCompletedCallback<PutObjectRequest, PutObjectResult>() {

                    @Override
                    public void onSuccess(PutObjectRequest request, PutObjectResult result) {
                        Log.d("onSuccess", "RequestId: " + result.getRequestId());

                        final Map<String, String> arguments = Maps.newHashMap();
                        arguments.put("success", "true");
                        arguments.put("instanceId", instanceId);
                        arguments.put("requestId", requestId);
                        arguments.put("bucket", bucket);
                        arguments.put("key", key);
                        invokeMethod("onUpload", arguments);
                    }

                    @Override
                    public void onFailure(PutObjectRequest request, ClientException clientException, ServiceException serviceException) {
                        final Map<String, String> arguments = Maps.newHashMap();
                        arguments.put("success", "false");
                        arguments.put("instanceId", instanceId);
                        arguments.put("requestId", requestId);
                        arguments.put("bucket", bucket);
                        arguments.put("key", key);

                        if (clientException != null) {
                            Log.w("onFailure", "ClientException: " + clientException.getMessage());

                            arguments.put("message", clientException.getMessage());
                        }

                        if (serviceException != null) {
                            Log.w("onFailure",
                                    "ServiceException: ErrorCode=" + serviceException.getErrorCode() +
                                            "RequestId" + serviceException.getRequestId() +
                                            "HostId" + serviceException.getHostId() +
                                            "RawMessage" + serviceException.getRawMessage());

                            arguments.put("message", serviceException.getRawMessage());
                        }

                        invokeMethod("onUpload", arguments);
                    }
                }
        );

        final Map<String, String> map = Maps.newHashMap();
        map.put("instanceId", instanceId);
        map.put("requestId", requestId);
        map.put("bucket", bucket);
        map.put("key", key);

        result.success(map);
    }

    private void delete(MethodCall call, MethodChannel.Result result) {
        if (oss == null) {
            init(call);
        }

        final String instanceId = call.argument("instanceId");
        final String requestId = call.argument("requestId");
        final String bucket = call.argument("bucket");
        final String key = call.argument("key");


        DeleteObjectRequest request = new DeleteObjectRequest(bucket, key);

        try {
            oss.deleteObject(request);
            final Map<String, String> map = Maps.newHashMap();
            map.put("instanceId", instanceId);
            map.put("requestId", requestId);
            map.put("bucket", bucket);
            map.put("key", key);

            result.success(map);
        } catch (ClientException e) {
            Log.w("deleteObject", "ClientException: " + e.getMessage());

            result.error("client error", e.getMessage(), null);
        } catch (ServiceException e) {
            Log.w("deleteObject", "ServiceException: " + e.getRawMessage());

            result.error("server error", e.getMessage(), e.getRawMessage());
        }
    }

    private void invokeMethod(final String method, final Object arguments) {
        registrar.activity().runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        channel.invokeMethod(method, arguments);
                    }
                });
    }
}
