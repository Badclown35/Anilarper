import SwiftUI

struct AnilarperRootView: View {
    var body: some View {
        TabView {
            HomeDashboardView()
                .tabItem { Label("Home", systemImage: "house") }
            LibraryView()
                .tabItem { Label("Library", systemImage: "books.vertical") }
            AnimeSearchView()
                .tabItem { Label("Discover", systemImage: "sparkles") }
            ReadingShelfView()
                .tabItem { Label("Reading", systemImage: "book.closed") }
            CustomListsView()
                .tabItem { Label("Lists", systemImage: "rectangle.stack") }
            QuestsView()
                .tabItem { Label("Quests", systemImage: "trophy") }
            StatisticsView()
                .tabItem { Label("Stats", systemImage: "chart.bar.xaxis") }
            ProfileDashboardView()
                .tabItem { Label("Profile", systemImage: "person.crop.circle") }
        }
        .tint(Color(red: 0.72, green: 0.55, blue: 1.0))
    }
}

private struct HomeDashboardView: View {
    @EnvironmentObject private var library: LibraryStore
    private var watching: [Anime] { library.anime.filter { $0.status == .watching } }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    VStack(alignment: .leading, spacing: 5) {
                        Text("YOUR ANIME SPACE").font(.caption.weight(.bold)).tracking(2).foregroundStyle(.purple)
                        Text("Welcome back.").font(.largeTitle.bold())
                        Text("Keep track of the stories you're following.").font(.subheadline).foregroundStyle(.secondary)
                    }
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                        MetricCard(title: "ANIME SAVED", value: "\(library.anime.count)", symbol: "books.vertical")
                        MetricCard(title: "WATCHING", value: "\(watching.count)", symbol: "play.circle")
                        MetricCard(title: "COMPLETED", value: "\(library.anime.filter { $0.status == .completed }.count)", symbol: "checkmark.circle")
                        MetricCard(title: "EPISODES", value: "\(library.anime.reduce(0) { $0 + $1.progress })", symbol: "tv")
                    }
                    if !watching.isEmpty {
                        Text("CONTINUE WATCHING").font(.caption.weight(.bold)).tracking(1.3).foregroundStyle(.purple)
                        ForEach(watching.prefix(5)) { item in
                            HStack(spacing: 11) {
                                AsyncImage(url: item.coverImageURL) { phase in
                                    if let image = phase.image { image.resizable().scaledToFill() }
                                    else { RoundedRectangle(cornerRadius: 8).fill(.white.opacity(0.08)) }
                                }
                                .frame(width: 48, height: 66).clipShape(RoundedRectangle(cornerRadius: 8))
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(item.title).font(.subheadline.weight(.semibold)).lineLimit(2)
                                    Text("Episode \(item.progress)").font(.caption).foregroundStyle(.purple)
                                }
                                Spacer()
                            }
                            .padding(10)
                            .background(.white.opacity(0.055), in: RoundedRectangle(cornerRadius: 13))
                        }
                    }
                    if library.anime.isEmpty {
                        ContentUnavailableView("Start your library", systemImage: "sparkles",
                            description: Text("Open Discover to search AniList and add your first anime."))
                            .padding(.top, 8)
                    }
                }
                .padding(18)
            }
            .navigationTitle("Anilarper")
        }
    }
}

private struct MetricCard: View {
    let title: String
    let value: String
    let symbol: String
    var body: some View {
        VStack(alignment: .leading, spacing: 9) {
            Image(systemName: symbol).font(.title3).foregroundStyle(.purple)
            Text(value).font(.title.bold().monospacedDigit())
            Text(title).font(.caption2.weight(.bold)).tracking(1).foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .background(.white.opacity(0.06), in: RoundedRectangle(cornerRadius: 15))
    }
}

private struct StatisticsView: View {
    @EnvironmentObject private var library: LibraryStore
    private var totalEpisodes: Int { library.anime.reduce(0) { $0 + $1.progress } }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text("Your statistics").font(.largeTitle.bold())
                    Text("A snapshot of your anime activity.").foregroundStyle(.secondary)
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                        MetricCard(title: "ANIME SAVED", value: "\(library.anime.count)", symbol: "bookmark")
                        MetricCard(title: "EPISODES", value: "\(totalEpisodes)", symbol: "play.rectangle")
                        MetricCard(title: "COMPLETED", value: "\(count(.completed))", symbol: "checkmark.circle")
                        MetricCard(title: "EST. HOURS", value: "\(totalEpisodes * 24 / 60)", symbol: "clock")
                    }
                    VStack(alignment: .leading, spacing: 13) {
                        Text("WATCH STATUS").font(.caption.weight(.bold)).tracking(1.3).foregroundStyle(.purple)
                        ForEach(AnimeStatus.allCases, id: \.self) { status in
                            let amount = count(status)
                            VStack(spacing: 5) {
                                HStack {
                                    Text(status.label)
                                    Spacer()
                                    Text("\(amount)").foregroundStyle(.secondary)
                                }.font(.subheadline)
                                GeometryReader { proxy in
                                    ZStack(alignment: .leading) {
                                        Capsule().fill(.white.opacity(0.08))
                                        Capsule().fill(.purple).frame(width: proxy.size.width * ratio(amount))
                                    }
                                }.frame(height: 6)
                            }
                        }
                    }
                    .padding(16)
                    .background(.white.opacity(0.06), in: RoundedRectangle(cornerRadius: 16))
                    Text("Estimated watch time assumes 24 minutes per episode.").font(.caption2).foregroundStyle(.secondary)
                }
                .padding(18)
            }
            .navigationTitle("Statistics")
        }
    }

    private func count(_ status: AnimeStatus) -> Int {
        library.anime.filter { $0.status == status }.count
    }
    private func ratio(_ value: Int) -> CGFloat {
        guard !library.anime.isEmpty else { return 0 }
        return CGFloat(value) / CGFloat(library.anime.count)
    }
}

private struct ProfileDashboardView: View {
    @EnvironmentObject private var library: LibraryStore
    var body: some View {
        NavigationStack {
            List {
                Section {
                    HStack(spacing: 14) {
                        Image(systemName: "person.crop.circle.fill").font(.system(size: 48)).foregroundStyle(.purple)
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Anime enthusiast").font(.headline)
                            Text("\(library.anime.count) titles in your collection").font(.caption).foregroundStyle(.secondary)
                        }
                    }.padding(.vertical, 8)
                }
                Section("Your collection") {
                    LabeledContent("Watching", value: "\(library.anime.filter { $0.status == .watching }.count)")
                    LabeledContent("Completed", value: "\(library.anime.filter { $0.status == .completed }.count)")
                    LabeledContent("Planning", value: "\(library.anime.filter { $0.status == .planning }.count)")
                }
                Section("About") {
                    LabeledContent("App", value: "Anilarper for iOS")
                    LabeledContent("Catalogue", value: "AniList")
                }
            }
            .navigationTitle("Profile")
        }
    }
}

#Preview {
    AnilarperRootView().environmentObject(LibraryStore()).environmentObject(ReadingStore()).environmentObject(CustomListsStore()).environmentObject(QuestStore())
}
