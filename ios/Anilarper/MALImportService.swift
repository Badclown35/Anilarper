import Foundation

struct MALImportSummary {
    let imported: Int
    let skipped: Int
    let total: Int
}

enum MALImportService {
    static func importXML(_ data: Data, using client: AniListClient,
                          into library: LibraryStore,
                          progress: @MainActor (Int, Int, String) -> Void) async throws -> MALImportSummary {
        let entries = try MALXMLParser.parse(data)
        var imported = 0
        var skipped = 0

        for (index, entry) in entries.enumerated() {
            if index > 0 { try? await Task.sleep(nanoseconds: 650_000_000) }
            await progress(index + 1, entries.count, entry.title)
            do {
                var match = try await client.findAnime(malId: entry.malId)
                if match == nil {
                    let candidates = try await client.searchAnime(entry.title)
                    let normalized = normalize(entry.title)
                    match = candidates.first(where: { candidate in
                        ([candidate.title.english, candidate.title.romaji, candidate.title.native]
                            .compactMap { $0 } + (candidate.synonyms ?? []))
                            .contains(where: { normalize($0) == normalized })
                    })
                }

                guard let match else {
                    skipped += 1
                    continue
                }

                let item = Anime(
                    id: match.id,
                    malId: entry.malId,
                    title: match.displayTitle,
                    nativeTitle: match.title.native,
                    coverImageURL: match.coverImage?.medium,
                    episodes: match.episodes,
                    score: entry.score > 0 ? Double(entry.score) : match.averageScore.map(Double.init),
                    status: mapStatus(entry.status),
                    progress: max(0, entry.watchedEpisodes)
                )
                await library.importAnime(item)
                imported += 1
            } catch {
                skipped += 1
            }
        }
        return MALImportSummary(imported: imported, skipped: skipped, total: entries.count)
    }

    private static func mapStatus(_ value: String) -> AnimeStatus {
        switch value.lowercased() {
        case "watching": .watching
        case "completed": .completed
        case "on-hold", "on hold": .paused
        case "dropped": .dropped
        default: .planning
        }
    }

    private static func normalize(_ value: String) -> String {
        value.folding(options: [.diacriticInsensitive, .caseInsensitive], locale: .current)
            .replacingOccurrences(of: "[^a-z0-9]", with: "", options: .regularExpression)
    }
}
