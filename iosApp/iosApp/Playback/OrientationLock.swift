import UIKit

/// The orientations the app allows right now. AppDelegate returns `mask` from
/// application(_:supportedInterfaceOrientationsFor:). Portrait everywhere except the player.
enum OrientationLock {
    static var mask: UIInterfaceOrientationMask = .portrait

    /// Sets `mask` and rotates now (iOS 16 API).
    static func apply(_ newMask: UIInterfaceOrientationMask, from viewController: UIViewController) {
        mask = newMask
        viewController.setNeedsUpdateOfSupportedInterfaceOrientations()
        viewController.view.window?.windowScene?.requestGeometryUpdate(.iOS(interfaceOrientations: newMask)) { error in
            NSLog("NoFlixPlayer: geometry update failed: %@", error.localizedDescription)
        }
    }
}
