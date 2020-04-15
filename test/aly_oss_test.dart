import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:aly_oss/aly_oss.dart';

void main() {
  const MethodChannel channel = MethodChannel('aly_oss');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await AlyOss.platformVersion, '42');
  });
}
