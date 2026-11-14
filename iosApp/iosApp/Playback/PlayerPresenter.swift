import UIKit
import Shared

/// The app's one IosPlaybackHost: presents the web player. App code creates it with PlayerPresenter() and
/// passes it to MainViewControllerKt.MainViewController(host:).
final class PlayerPresenter: IosPlaybackHost {
    init() {}

    func presentPlayer(request: IosPlayerRequest) {
        // Kotlin calls this on the main thread; hop only if a caller ever doesn't.
        guard Thread.isMainThread else {
            DispatchQueue.main.async { self.presentPlayer(request: request) }
            return
        }
        guard let top = Self.topViewController() else {
            NSLog("NoFlixPlayer: no view controller to present from")
            return
        }
        if top is PlayerViewController || top.presentedViewController is PlayerViewController {
            NSLog("NoFlixPlayer: player already presented")
            return
        }
        NSLog("NoFlixPlayer: presentPlayer %@", request.url)
        let player = PlayerViewController(request: request)
        player.modalPresentationStyle = .fullScreen
        top.present(player, animated: true)
    }

    /// The top-most view controller of the foreground key window (the Compose view controller, or a sheet over it).
    private static func topViewController() -> UIViewController? {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        let scene = scenes.first { $0.activationState == .foregroundActive } ?? scenes.first
        let window = scene?.windows.first { $0.isKeyWindow } ?? scene?.windows.first
        var top = window?.rootViewController
        while let presented = top?.presentedViewController, !presented.isBeingDismissed {
            top = presented
        }
        return top
    }
}
