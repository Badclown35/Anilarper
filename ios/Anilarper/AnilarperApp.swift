import SwiftUI

@main
struct AnilarperApp: App {
    var body: some Scene {
        WindowGroup {
            AnilarperRootView()
                .environmentObject(LibraryStore())
                .environmentObject(ReadingStore())
                .environmentObject(CustomListsStore())
                .preferredColorScheme(.dark)
        }
    }
}
