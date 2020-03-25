import 'package:flutter/services.dart';

// Corresponds to BackgroundLocationHandler
class BackgroundLocation {
  // This is also defined in BackgroundLocationHandler.kt
  static const CHANNEL_NAME = 'events.pandemic.covid19/background_location';
  static const MethodChannel _channel = MethodChannel(CHANNEL_NAME);

  static stop() {
    _channel.invokeMethod('stop');
  }

  static start(callback) {
    var stream = LocationStream().stream;
    stream.receiveBroadcastStream("1").listen(callback);

    _channel.invokeMethod('start');
  }

  static Future<bool> isRunning() async {
    return await _channel.invokeMethod('status');
  }
}

class LocationStream {
  static LocationStream _instance;

  EventChannel stream;

  factory LocationStream() {
    if (_instance == null) {
      var stream = const EventChannel("events.pandemic.covid19/location_event");
      _instance = LocationStream.private(stream);
    }
    return _instance;
  }

  LocationStream.private(this.stream);

}