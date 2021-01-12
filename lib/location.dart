import 'dart:async';

import 'package:location_platform_interface/location_platform_interface.dart';
export 'package:location_platform_interface/location_platform_interface.dart'
    show LocationModel, LocationMode;

class Location {
  factory Location() => instance;

  Location._();

  static final Location instance = Location._();

  Future<bool> setSetting({
    LocationMode mode = LocationMode.high,
    int interval = 1000,
    double distanceFilter = 0,
  }) {
    return LocationPlatform.instance.setSettings(
      mode: mode,
      interval: interval,
      distanceFilter: distanceFilter,
    );
  }

  Future<bool> isBackground() {
    return LocationPlatform.instance.isBackground();
  }

  Future<bool> setBackground({bool enable}) {
    return LocationPlatform.instance.setBackground(enable: enable);
  }

  Future<LocationModel> fetchLocation() {
    return LocationPlatform.instance.fetchLocation();
  }

  Future<bool> checkLocationService() {
    return LocationPlatform.instance.checkLocationService();
  }

  Stream<LocationModel> get onLocation {
    return LocationPlatform.instance.onLocation;
  }
}
