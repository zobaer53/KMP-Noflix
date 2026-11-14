import SwiftUI
import Shared

@main
struct NoFlixApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    init() {
        // The key flows Secrets.xcconfig -> Config.xcconfig -> Info.plist (TMDBApiKey). Empty without a key: TMDB answers 401.
        let apiKey = Bundle.main.object(forInfoDictionaryKey: "TMDBApiKey") as? String ?? ""
        IosKoinKt.startKoinIos(apiKey: apiKey)
    }

    var body: some Scene {
        WindowGroup {
            ComposeRootView()
                .ignoresSafeArea()
        }
    }
}
