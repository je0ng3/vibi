import AuthenticationServices
import Cmp
import UIKit

/// Swift implementation of the Kotlin `AppleSignInBridge` interface (defined in
/// `:shared` commonMain). 호출자(`IosAppleSignInClient`)가 callback 을 suspend
/// 함수로 변환한다.
///
/// AuthenticationServices.framework 는 시스템 프레임워크라 SPM 등록 불필요.
/// Sign in with Apple capability 가 entitlements 에 있어야 런타임 동작.
final class AppleSignInBridgeImpl: NSObject, AppleSignInBridge,
    ASAuthorizationControllerDelegate, ASAuthorizationControllerPresentationContextProviding {

    /// 한 번에 하나의 시트만 띄움 — 사용자가 Apple sheet 보는 도중 다시 누를 수 없으므로 OK.
    private var pendingCallback: ((String?, String?, String?) -> Void)?

    func signIn(callback: @escaping (String?, String?, String?) -> Void) {
        let request = ASAuthorizationAppleIDProvider().createRequest()
        request.requestedScopes = [.fullName, .email]
        let controller = ASAuthorizationController(authorizationRequests: [request])
        controller.delegate = self
        controller.presentationContextProvider = self
        pendingCallback = callback
        controller.performRequests()
    }

    // MARK: ASAuthorizationControllerDelegate

    func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization authorization: ASAuthorization
    ) {
        defer { pendingCallback = nil }
        guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential else {
            pendingCallback?(nil, nil, "unexpected_credential_type")
            return
        }
        guard let tokenData = credential.identityToken,
              let idToken = String(data: tokenData, encoding: .utf8), !idToken.isEmpty else {
            pendingCallback?(nil, nil, "missing_identity_token")
            return
        }
        // fullName 은 최초 1회만 채워지며, 그 외에는 모든 필드가 nil 이라 formatter 가 빈 문자열을 반환할 수 있음.
        let formatted: String? = {
            guard let components = credential.fullName else { return nil }
            let formatter = PersonNameComponentsFormatter()
            let s = formatter.string(from: components)
            return s.isEmpty ? nil : s
        }()
        pendingCallback?(idToken, formatted, nil)
    }

    func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError error: Error
    ) {
        defer { pendingCallback = nil }
        let asError = error as? ASAuthorizationError
        let code: String
        switch asError?.code {
        case .canceled: code = "user_canceled"
        case .failed: code = "apple_sign_in_failed"
        case .invalidResponse: code = "invalid_response"
        case .notHandled: code = "not_handled"
        case .unknown, .none: code = "unknown_error"
        @unknown default: code = "unknown_error"
        }
        pendingCallback?(nil, nil, "\(code): \(error.localizedDescription)")
    }

    // MARK: ASAuthorizationControllerPresentationContextProviding

    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        topWindow() ?? ASPresentationAnchor()
    }

    private func topWindow() -> UIWindow? {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }?
            .windows
            .first(where: { $0.isKeyWindow })
    }
}
