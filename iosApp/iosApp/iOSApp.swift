import SwiftUI
import GoogleMaps

@main
struct iOSApp: App {
    init() {
        // Načtení API klíče z Info.plist (který ho bere z Secrets.xcconfig)
        let apiKey = Bundle.main.object(forInfoDictionaryKey: "MAPS_API_KEY") as? String ?? ""
        GMSServices.provideAPIKey(apiKey)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}