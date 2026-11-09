# Anilarper for iOS

The iOS target is a native SwiftUI app. It is built independently from the Android app.

## Simulator build

Every iOS source change runs the **Build Anilarper iOS** GitHub Actions workflow. The artifact named `Anilarper-iOS-Simulator` is for the iOS Simulator and is not installable on a physical iPhone.

## Signed IPA build

The workflow can also export an iPhone IPA when manually dispatched with **Build signed iPhone IPA** enabled.

This requires an Apple Developer team and matching App Store distribution signing assets. Add these repository Actions secrets:

- `IOS_DISTRIBUTION_CERTIFICATE_BASE64`: base64-encoded Apple Distribution certificate exported as a password-protected `.p12`.
- `IOS_DISTRIBUTION_CERTIFICATE_PASSWORD`: password used when exporting that `.p12`.
- `IOS_PROVISIONING_PROFILE_BASE64`: base64-encoded App Store provisioning profile for the app's bundle identifier.
- `IOS_TEAM_ID`: Apple Developer Team ID.
- `IOS_BUNDLE_ID`: bundle identifier registered with that team and included in the provisioning profile.

Never commit signing certificates, provisioning profiles, passwords, or API keys to the repository. The signed artifact is named `Anilarper-iOS-IPA`.

The exported IPA uses App Store Connect distribution and can be submitted to TestFlight/App Store Connect using Apple's distribution tools. Direct device installation requires an appropriate Ad Hoc or Development provisioning profile and registered device.
