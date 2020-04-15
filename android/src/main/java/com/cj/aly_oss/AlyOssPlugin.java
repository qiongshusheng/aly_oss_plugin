package com.cj.aly_oss;

import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/** AliyunossPlugin */
public class AlyOssPlugin {
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "aly_oss");
    channel.setMethodCallHandler(new MethodCallHandlerImpl(channel, registrar));
  }

}
