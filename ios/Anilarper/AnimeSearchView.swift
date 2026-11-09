import SwiftUI

struct AnimeSearchView: View {
    @EnvironmentObject private var library: LibraryStore
    @State private var query = ""
    @State private var results: [AnimeSearchResult] = []
    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var searched = false
    @State private var isTrending = true

    private let client = AniListClient()

    var body: some View {
        NavigationStack {
            VStack(spacing: 14) {
                HStack(spacing: 10) {
                    Image(systemName: "magnifyingglass").foregroundStyle(.secondary)
                    TextField("Search anime", text: $query)
                        .submitLabel(.search)
                        .onSubmit { Task { await search() } }
                    if !query.isEmpty {
                        Button { query = ""; results = []; searched = false; Task { await loadTrending() } } label: {
                            Image(systemName: "xmark.circle.fill").foregroundStyle(.secondary)
                        }
                    }
                }
                .padding(13)
                .background(.white.opacity(0.07), in: RoundedRectangle(cornerRadius: 14))

                if isLoading {
                    ProgressView("Searching AniList…").tint(.purple).padding(.top, 28)
                    Spacer()
                } else if let errorMessage {
                    ContentUnavailableView("Search failed", systemImage: "wifi.exclamation",
                                           description: Text(errorMessage))
                    Spacer()
                } else if results.isEmpty {
                    ContentUnavailableView(searched ? "No results" : "Find your next anime",
                                           systemImage: "sparkles",
                                           description: Text(searched ? "Try another title." : "Search the AniList catalogue and add titles to your library."))
                    Spacer()
                } else {
                    List {
                        Section(isTrending ? "TRENDING NOW" : "SEARCH RESULTS") {
                            ForEach(results) { result in
                                SearchResultRow(result: result, isSaved: library.anime.contains { $0.id == result.id }) {
                                    library.add(result.asAnime)
                                }
                                .listRowBackground(Color.clear)
                            }
                        }
                    }
                    .listStyle(.plain)
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 12)
            .navigationTitle("Discover")
            .onAppear {
                if results.isEmpty && !searched && !isLoading {
                    Task { await loadTrending() }
                }
            }
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { Task { if query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { await loadTrending() } else { await search() } } } label: {
                        Image(systemName: "arrow.clockwise")
                    }
                    .disabled(isLoading)
                }
            }
        }
    }

    @MainActor
    private func loadTrending() async {
        isLoading = true
        errorMessage = nil
        isTrending = true
        defer { isLoading = false }
        do {
            results = try await client.trendingAnime()
        } catch {
            errorMessage = "Couldn't load trending anime. Check your connection and try again."
        }
    }

    @MainActor
    private func search() async {
        let term = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !term.isEmpty else { return }
        isLoading = true
        errorMessage = nil
        searched = true
        isTrending = false
        defer { isLoading = false }
        do {
            results = try await client.searchAnime(term)
        } catch {
            errorMessage = "Couldn't reach AniList. Check your connection and try again."
        }
    }
}

private struct SearchResultRow: View {
    let result: AnimeSearchResult
    let isSaved: Bool
    let onAdd: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            AsyncImage(url: result.coverImage?.medium) { phase in
                if let image = phase.image {
                    image.resizable().scaledToFill()
                } else {
                    RoundedRectangle(cornerRadius: 9).fill(.white.opacity(0.08))
                        .overlay(Image(systemName: "play.rectangle").foregroundStyle(.secondary))
                }
            }
            .frame(width: 62, height: 88)
            .clipShape(RoundedRectangle(cornerRadius: 9))

            VStack(alignment: .leading, spacing: 5) {
                Text(result.displayTitle).font(.subheadline.weight(.semibold)).lineLimit(2)
                if let romaji = result.title.romaji, romaji != result.displayTitle {
                    Text(romaji).font(.caption).foregroundStyle(.secondary).lineLimit(1)
                }
                Text([result.episodes.map { "\($0) eps" }, result.averageScore.map { "Score \($0)%" }]
                    .compactMap { $0 }.joined(separator: " · "))
                    .font(.caption2).foregroundStyle(.secondary)
                Spacer(minLength: 0)
                Button(action: onAdd) {
                    Label(isSaved ? "In library" : "Add to library",
                          systemImage: isSaved ? "checkmark.circle.fill" : "plus.circle.fill")
                        .font(.caption.weight(.semibold))
                }
                .buttonStyle(.borderedProminent)
                .tint(isSaved ? .gray : .purple)
                .disabled(isSaved)
            }
            Spacer(minLength: 0)
        }
        .padding(.vertical, 5)
    }
}

private extension AnimeSearchResult {
    var asAnime: Anime {
        Anime(id: id, malId: idMal, title: displayTitle,
              nativeTitle: title.native, coverImageURL: coverImage?.medium,
              episodes: episodes, score: averageScore.map(Double.init),
              status: .planning, progress: 0)
    }
}
