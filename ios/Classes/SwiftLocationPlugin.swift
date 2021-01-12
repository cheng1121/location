import Flutter
import UIKit
import CoreLocation

public class SwiftLocationPlugin: NSObject, FlutterPlugin,CLLocationManagerDelegate {
    //声明定位管理器
    let locationManager:CLLocationManager = CLLocationManager()
    
  var flutterResult: FlutterResult?
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "com.bd.cheng/location", binaryMessenger: registrar.messenger())
    let instance = SwiftLocationPlugin()
    instance.setLocationMananger()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
     flutterResult = result
    switch call.method {
    case "fetchLocation":
        fetchLocation()
        break
    case "checkLocationService":
        checkLocationService()
       break
    default:
        result(FlutterMethodNotImplemented)
        break
    }
  }
    
    //获取定位
    private func fetchLocation(){
         
        let permission = checkPermission()
        if(!permission){
            //请求定位权限
            locationManager.requestWhenInUseAuthorization()
        }else{
            requestLocation()
        }
        
        
    }
    
    
    //检查是否开启定位服务
    private func checkLocationService(){
        if CLLocationManager.locationServicesEnabled(){
            flutterResult!(true)
        }else{
            flutterResult!(false)
        }
    }
    
    private func requestLocation(){
        //获取当前定位
        if #available(iOS 9.0, *) {
            locationManager.requestLocation()
        } else {
          flutterResult!(FlutterError(code: "1001", message: "该设备不支持定位", details: ""))
        }
    }
    
    
    private func checkPermission() -> Bool{
        switch CLLocationManager.authorizationStatus() {
        case .notDetermined:
           return false
        case .restricted:
            
            return false
        case .authorizedWhenInUse,.authorizedAlways:
            return true
        default:
            return false
        }
        
    }
    
    func setLocationMananger() {
        //位置改变300米时才调用一次定位方法
        locationManager.distanceFilter = 300
        //精度
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        //代理
        locationManager.delegate = self
      
    }
    
    //定位回调
    public func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        let lastLocation = locations.last!
        print("location ======\(lastLocation)")
        let map = toMap(location: lastLocation)
        flutterResult!(map)
    }
    //定位出错
    public func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        flutterResult!(FlutterError(code: "1003", message: error.localizedDescription, details: ""))
    }
    
    public func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        
        let result = checkPermission()
        print("定位权限改变回调 ==\(result)")
        if(result){
            requestLocation()
        }
    }
    
    
    //位置信息转为map
    private func toMap(location:CLLocation)-> [String:Double]{
        var map = [String:Double]()
        
        map["latitude"] = location.coordinate.latitude
        map["longitude"] = location.coordinate.longitude
        map["altitude"] = location.altitude
        map["speed"] = location.speed
        if #available(iOS 10.0, *) {
            map["speed_accuracy"] = location.speedAccuracy
        }
        map["heading"] = location.course
        map["time"] = location.timestamp.timeIntervalSince1970
        
        return map
    }
    
    
}
