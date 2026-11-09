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

    enum CodingKeys: String, CodingKey {
        case id, malId, title, nativeTitle, coverImageURL, episodes, score, status, progress
    }
}
