import Foundation

struct ReadingTitle: Identifiable, Codable, Hashable {
    let id: Int
    var title: String
    var nativeTitle: String?
    var coverImageURL: URL?
    var format: String
    var status: String
    var chapters: Int?
    var volumes: Int?
    var description: String
    var kind: ReadingKind
    var progress: Int
    var volumeProgress: Int
    var readingStatus: ReadingStatus
}

enum ReadingKind: String, Codable, CaseIterable {
    case manga
    case novel

    var label: String { self == .manga ? "Manga" : "Light novels" }
}

enum ReadingStatus: String, Codable, CaseIterable {
    case planning
    case reading
    case completed
    case paused
    case dropped

    var label: String {
        switch self {
        case .planning: "Planning"
        case .reading: "Reading"
        case .completed: "Completed"
        case .paused: "Paused"
        case .dropped: "Dropped"
        }
    }
}
