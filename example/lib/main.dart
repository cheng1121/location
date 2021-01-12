import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:location/location.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  LocationModel _locationModel;

  @override
  void initState() {
    super.initState();
    // initPlatformState();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              OutlinedButton(
                onPressed: () async {
                  final bool opened =
                      await Location.instance.checkLocationService();
                  print('是否已开启位置服务.........$opened');

                  if (opened) {
                    final LocationModel model =
                        await Location.instance.fetchLocation();
                    print(
                        'model ======${model.longitude}=====${model.latitude}');
                    setState(() {
                      _locationModel = model;
                    });
                  }
                },
                child: const Text('获取定位'),
              ),
              if (_locationModel != null)
                Text(
                  '位置信息:${_locationModel.toString()}',
                  textAlign: TextAlign.center,
                ),
            ],
          ),
        ),
      ),
    );
  }
}
