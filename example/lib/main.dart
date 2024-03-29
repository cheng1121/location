import 'package:flutter/material.dart';
import 'package:location/location.dart';
import 'package:permission_handler/permission_handler.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  LocationModel? _locationModel;

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
                    final PermissionStatus result =
                        await Permission.locationWhenInUse.request();
                    print('permission is granted : $result');
                    if (result.isGranted) {
                      final LocationModel? model =
                          await Location.instance.fetchLocation();
                      if (model != null) {
                        print(
                            'model ======${model.longitude}=====${model.latitude}');
                        setState(() {
                          _locationModel = model;
                        });
                      }
                    }
                  }
                },
                child: const Text('获取定位'),
              ),
              Text(
                '位置信息:${_locationModel?.toString()}',
                textAlign: TextAlign.center,
              )
            ],
          ),
        ),
      ),
    );
  }
}
