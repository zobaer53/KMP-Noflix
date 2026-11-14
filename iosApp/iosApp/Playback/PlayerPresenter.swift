import UIKit
import Shared

/// The app's one IosPlaybackHost: presents the web player. App code creates it with PlayerPresenter() and
/// passes it to MainViewControllerKt.MainViewController(host:). I3 implements it; keep this signature.
final class PlayerPresenter: IosPlaybackHost {
    init() {}
    func presentPlayer(request: IosPlayerRequest) {
        NSLog("NoFlixPlayer: presentPlayer %@ (stub)", request.url)
    }
}
