import SwiftUI

struct AnilarperRootView: View {
    var body: some View {
        TabView {
            HomeView()
                .tabItem { Label("Home", systemImage: "house") }

            LibraryView()
                .tabItem { Label("Library", systemImage: "books.vertical") }

            DiscoverView()
                .tabItem { Label("Discover", systemImage: "sparkles") }

            ProfileView()
                .tabItem { Label("Profile", systemImage: "person.crop.circle") }
        }
        .tint(Color(red: 0.72, green: 0.55, blue: 1.0))
    }
}

private struct HomeView: View {
    var body: some View {
        NavigationStack {
            ContentUnavailableView(
                "Your anime space",
                systemImage: "play.rectangle.on.rectangle",
                description: Text("Your Anilarper library and activity will appear here.")
            )
            .navigationTitle("Anilarper")
        }
    }
}

private struct LibraryView: View {
    var body: some View {
        NavigationStack {
            ContentUnavailableView(
                "Your library is ready",
                systemImage: "books.vertical",
                description: Text("Anime import, lists, and tracking are coming in the iOS port.")
            )
            .navigationTitle("Library")
        }
    }
}

private struct DiscoverView: View {
    var body: some View {
        NavigationStack {
            ContentUnavailableView(
                "Discover anime",
                systemImage: "sparkles",
                description: Text("AniList-powered discovery is part of the next porting milestone.")
            )
            .navigationTitle("Discover")
        }
    }
}

private struct ProfileView: View {
    var body: some View {
        NavigationStack {
            ContentUnavailableView(
                "Your profile",
                systemImage: "person.crop.circle",
                description: Text("Stats, profile customization, and MAL tools will be added here.")
            )
            .navigationTitle("Profile")
        }
    }
}

#Preview {
    AnilarperRootView()
}
