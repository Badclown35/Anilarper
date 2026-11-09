import SwiftUI

@main
struct AnilarperApp: App {
    var body: some Scene {
        WindowGroup {
            AnilarperRootView()
                .environmentObject(LibraryStore())
                .preferredColorScheme(.dark)
        }
    }
}
