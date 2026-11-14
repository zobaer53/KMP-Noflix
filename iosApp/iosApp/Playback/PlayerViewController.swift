import UIKit
import WebKit
import Shared

/// The full-screen web player (doc/kmp-ios-plan.md, "Playback architecture on iOS").
/// The decisions (which URL may load, which page is a watch page) and the injected scripts come from the shared
/// Kotlin code (StreamPageKt / IosPlayerRequest), so one fix covers Android and iOS. Everything it logs starts with
/// "NoFlixPlayer:" so a tester can follow it:
///   xcrun simctl spawn booted log stream --level debug --predicate 'eventMessage CONTAINS "NoFlixPlayer"'
final class PlayerViewController: UIViewController {
    /// Bump the number on every edit of BlockRules.json: compiled lists are cached by identifier.
    private static let ruleListIdentifier = "noflix-block-v1"
    /// Launch argument `-NoFlixDisableBlockRules YES` loads the player without the content rule list (diagnosis).
    private static let disableRulesKey = "NoFlixDisableBlockRules"

    private let request: IosPlayerRequest
    private var webView: WKWebView!
    private var urlObservation: NSKeyValueObservation?
    private var requestedMask: UIInterfaceOrientationMask?

    init(request: IosPlayerRequest) {
        self.request = request
        super.init(nibName: nil, bundle: nil)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init(coder:) is not used") }

    // MARK: Lifecycle

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .black

        let controller = WKUserContentController()
        controller.addUserScript(WKUserScript(source: request.documentStartScript, injectionTime: .atDocumentStart, forMainFrameOnly: true))
        controller.addUserScript(WKUserScript(source: request.pageScript, injectionTime: .atDocumentEnd, forMainFrameOnly: true))

        let configuration = WKWebViewConfiguration()
        configuration.userContentController = controller
        configuration.allowsInlineMediaPlayback = true
        configuration.mediaTypesRequiringUserActionForPlayback = []
        configuration.preferences.javaScriptCanOpenWindowsAutomatically = false
        configuration.preferences.isElementFullscreenEnabled = true

        let webView = WKWebView(frame: view.bounds, configuration: configuration)
        webView.translatesAutoresizingMaskIntoConstraints = false
        webView.navigationDelegate = self
        webView.uiDelegate = self
        webView.allowsBackForwardNavigationGestures = true
        webView.isOpaque = false
        webView.backgroundColor = .black
        webView.scrollView.backgroundColor = .black
        // Edge to edge like the Android player: the page's own CSS stretches the player over the viewport.
        webView.scrollView.contentInsetAdjustmentBehavior = .never
        webView.accessibilityLabel = request.title
        #if DEBUG
        if #available(iOS 16.4, *) { webView.isInspectable = true }
        #endif
        view.addSubview(webView)
        NSLayoutConstraint.activate([
            webView.topAnchor.constraint(equalTo: view.topAnchor),
            webView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            webView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            webView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
        ])
        self.webView = webView

        addCloseButton()

        // The site is a SPA: pushState changes the URL without didFinish, so watch the URL itself.
        urlObservation = webView.observe(\.url, options: [.new]) { [weak self] _, _ in
            self?.updateOrientation()
        }

        // Load the page only after the rule list is ready (milliseconds when cached).
        Self.loadRuleList { [weak self] ruleList in
            guard let self else { return }
            if let ruleList { controller.add(ruleList) }
            guard let url = URL(string: self.request.url) else {
                NSLog("NoFlixPlayer: invalid player url")
                return
            }
            NSLog("NoFlixPlayer: loading %@", url.host ?? "?")
            self.webView.load(URLRequest(url: url))
        }
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        updateOrientation()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        if isBeingDismissed { restorePortrait(presenter: presentingViewController) }
    }

    deinit {
        urlObservation?.invalidate()
    }

    // MARK: Appearance and orientation

    override var supportedInterfaceOrientations: UIInterfaceOrientationMask { OrientationLock.mask }
    override var prefersStatusBarHidden: Bool { true }
    override var prefersHomeIndicatorAutoHidden: Bool { true }

    /// Landscape on the stream site's /watch/ pages, portrait everywhere else (the Android behavior).
    private func updateOrientation() {
        let mask: UIInterfaceOrientationMask = StreamPageKt.isStreamWatchPage(url: webView?.url?.absoluteString) ? .landscape : .portrait
        guard mask != requestedMask else { return }
        requestedMask = mask
        NSLog("NoFlixPlayer: orientation %@", mask == .landscape ? "landscape" : "portrait")
        OrientationLock.apply(mask, from: self)
    }

    private func restorePortrait(presenter: UIViewController?) {
        OrientationLock.apply(.portrait, from: presenter ?? self)
    }

    // MARK: Close button

    private func addCloseButton() {
        var configuration = UIButton.Configuration.filled()
        configuration.image = UIImage(systemName: "xmark")
        configuration.baseForegroundColor = .white
        configuration.baseBackgroundColor = UIColor.black.withAlphaComponent(0.5)
        configuration.cornerStyle = .capsule
        let button = UIButton(configuration: configuration, primaryAction: UIAction { [weak self] _ in self?.close() })
        button.translatesAutoresizingMaskIntoConstraints = false
        button.accessibilityIdentifier = "player_close"
        button.accessibilityLabel = "Close"
        view.addSubview(button)
        NSLayoutConstraint.activate([
            button.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 8),
            button.leadingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.leadingAnchor, constant: 8),
            button.widthAnchor.constraint(equalToConstant: 44),
            button.heightAnchor.constraint(equalToConstant: 44),
        ])
    }

    /// Always dismisses the player (in-page history is the edge swipe's job).
    private func close() {
        NSLog("NoFlixPlayer: close")
        let presenter = presentingViewController
        webView.stopLoading()
        webView.pauseAllMediaPlayback {}
        restorePortrait(presenter: presenter)
        dismiss(animated: true) { [weak self] in
            // The presenter (Compose) view controller now owns the rotation: tell it portrait is the only mask.
            self?.restorePortrait(presenter: presenter)
        }
    }

    // MARK: Content rule list

    /// The small bundled list of third-party ad and pop-under hosts (BlockRules.json): cached in WebKit's store by
    /// identifier. Calls [completion] on the main thread with nil if the list is disabled, missing or doesn't compile.
    private static func loadRuleList(completion: @escaping (WKContentRuleList?) -> Void) {
        if UserDefaults.standard.bool(forKey: disableRulesKey) {
            NSLog("NoFlixPlayer: block rules disabled by -%@", disableRulesKey)
            completion(nil)
            return
        }
        let store: WKContentRuleListStore? = WKContentRuleListStore.default()
        guard let store else {
            NSLog("NoFlixPlayer: no content rule list store")
            completion(nil)
            return
        }
        store.lookUpContentRuleList(forIdentifier: ruleListIdentifier) { cached, _ in
            if let cached {
                NSLog("NoFlixPlayer: block rules loaded from cache")
                DispatchQueue.main.async { completion(cached) }
                return
            }
            guard let url = Bundle.main.url(forResource: "BlockRules", withExtension: "json"),
                  let json = try? String(contentsOf: url, encoding: .utf8) else {
                NSLog("NoFlixPlayer: BlockRules.json is missing from the bundle")
                DispatchQueue.main.async { completion(nil) }
                return
            }
            store.compileContentRuleList(forIdentifier: ruleListIdentifier, encodedContentRuleList: json) { compiled, error in
                if let error {
                    NSLog("NoFlixPlayer: block rules failed to compile: %@", error.localizedDescription)
                } else {
                    NSLog("NoFlixPlayer: block rules compiled")
                }
                DispatchQueue.main.async { completion(compiled) }
            }
        }
    }
}

// MARK: - WKNavigationDelegate

extension PlayerViewController: WKNavigationDelegate {
    func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction,
                 decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        let url = navigationAction.request.url
        let isMainFrame = navigationAction.targetFrame?.isMainFrame ?? true
        if StreamPageKt.isStreamNavigationAllowed(url: url?.absoluteString, isMainFrame: isMainFrame) {
            decisionHandler(.allow)
        } else {
            NSLog("NoFlixPlayer: cancelled navigation %@ (%@)", url?.host ?? url?.scheme ?? "?", isMainFrame ? "main frame" : "sub frame")
            decisionHandler(.cancel)
        }
    }

    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        // Same as Android's onPageFinished: the script is idempotent, and the SPA may have replaced the DOM.
        guard StreamPageKt.isStreamSitePage(url: webView.url?.absoluteString) else { return }
        webView.evaluateJavaScript(request.pageScript) { _, error in
            if let error { NSLog("NoFlixPlayer: page script failed: %@", error.localizedDescription) }
        }
    }

    func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
        NSLog("NoFlixPlayer: navigation failed: %@", error.localizedDescription)
    }

    func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
        NSLog("NoFlixPlayer: navigation failed: %@", error.localizedDescription)
    }

    func webViewWebContentProcessDidTerminate(_ webView: WKWebView) {
        NSLog("NoFlixPlayer: web content process terminated, reloading")
        webView.reload()
    }
}

// MARK: - WKUIDelegate

extension PlayerViewController: WKUIDelegate {
    /// Pop-ups and pop-unders: never open a window.
    func webView(_ webView: WKWebView, createWebViewWith configuration: WKWebViewConfiguration,
                 for navigationAction: WKNavigationAction, windowFeatures: WKWindowFeatures) -> WKWebView? {
        NSLog("NoFlixPlayer: blocked popup %@", navigationAction.request.url?.host ?? "?")
        return nil
    }

    // Ads use alert/confirm/prompt: complete at once with the "dismissed" answer, no UI.
    func webView(_ webView: WKWebView, runJavaScriptAlertPanelWithMessage message: String,
                 initiatedByFrame frame: WKFrameInfo, completionHandler: @escaping () -> Void) {
        NSLog("NoFlixPlayer: suppressed alert")
        completionHandler()
    }

    func webView(_ webView: WKWebView, runJavaScriptConfirmPanelWithMessage message: String,
                 initiatedByFrame frame: WKFrameInfo, completionHandler: @escaping (Bool) -> Void) {
        NSLog("NoFlixPlayer: suppressed confirm")
        completionHandler(false)
    }

    func webView(_ webView: WKWebView, runJavaScriptTextInputPanelWithPrompt prompt: String, defaultText: String?,
                 initiatedByFrame frame: WKFrameInfo, completionHandler: @escaping (String?) -> Void) {
        NSLog("NoFlixPlayer: suppressed prompt")
        completionHandler(nil)
    }

    func webView(_ webView: WKWebView, requestMediaCapturePermissionFor origin: WKSecurityOrigin,
                 initiatedByFrame frame: WKFrameInfo, type: WKMediaCaptureType,
                 decisionHandler: @escaping (WKPermissionDecision) -> Void) {
        decisionHandler(.deny)
    }
}
