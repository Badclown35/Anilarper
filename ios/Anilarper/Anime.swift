import Foundation

enum AnimeStatus: String, Codable, CaseIterable {
    case planning
    case watching
    case completed
    case paused
    case dropped

    var label: String {
        switch self {
        case .planning: "Planning"
        case .watching: "Watching"
        case .completed: "Completed"
        case .paused: "Paused"
        case .dropped: "Dropped"
        }
    }
}

struct Anime: Identifiable, Codable, Hashable {
    let id: Int
    var malId: Int?
    var title: String
    var nativeTitle: String?
    var coverImageURL: URL?
    var episodes: Int?
    var score: Double?
    var status: AnimeStatus
    var progress: Int
    var format: String? = nil
    var airingStatus: String? = nil
    var description: String? = nil
    var season: String? = nil
    var seasonYear: Int? = nil
    var genres: [String]? = nil
    var studios: [String]? = nil
    var lastWatchedAt: Int64? = nil

    enum CodingKeys: String, CodingKey {
        case id, malId, title, nativeTitle, coverImageURL, episodes, score, status, progress
        case format, airingStatus, description, season, seasonYear, genres, studios, lastWatchedAt
    }
}
