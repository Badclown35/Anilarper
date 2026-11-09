import Foundation
import SwiftUI

struct LocalAudioTrack: Identifiable, Codable, Hashable {
    let id: UUID
    var title: String
    var filename: String
}

struct MusicLink: Identifiable, Codable, Hashable {
    let id: UUID
    var title: String
    var url: String
}

@MainActor
final class MusicLibraryStore: ObservableObject {
    @Published private(set) var tracks: [LocalAudioTrack] = []
    @Published private(set) var links: [MusicLink] = []
    private let tracksKey = "anilarper.ios.audioTracks.v1"
    private let linksKey = "anilarper.ios.musicLinks.v1"

    init() {
        tracks = decode(LocalAudioTrack.self, key: tracksKey)
        links = decode(MusicLink.self, key: linksKey)
    }

    func importAudio(from source: URL) throws {
        let folder = try audioFolder()
        let id = UUID()
        let ext = source.pathExtension.isEmpty ? "audio" : source.pathExtension
        let filename = id.uuidString + "." + ext
        let destination = folder.appendingPathComponent(filename)
        try FileManager.default.copyItem(at: source, to: destination)
        let title = source.deletingPathExtension().lastPathComponent
        tracks.append(LocalAudioTrack(id: id, title: title, filename: filename))
        save()
    }

    func remove(_ track: LocalAudioTrack) {
        let url = try? audioFolder().appendingPathComponent(track.filename)
        if let url { try? FileManager.default.removeItem(at: url) }
        tracks.removeAll { $0.id == track.id }
        save()
    }

    func addLink(title: String, url: String) throws {
        let cleanTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let parsed = URL(string: url), ["https", "http"].contains(parsed.scheme?.lowercased() ?? ""),
              !cleanTitle.isEmpty else { throw MusicLibraryError.invalidLink }
        guard !links.contains(where: { $0.url == parsed.absoluteString }) else { return }
        links.append(MusicLink(id: UUID(), title: cleanTitle, url: parsed.absoluteString))
        save()
    }

    func remove(_ link: MusicLink) {
        links.removeAll { $0.id == link.id }
        save()
    }

    func fileURL(for track: LocalAudioTrack) throws -> URL {
        try audioFolder().appendingPathComponent(track.filename)
    }

    private func audioFolder() throws -> URL {
        let documents = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let folder = documents.appendingPathComponent("AnilarperAudio", isDirectory: true)
        try FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
        return folder
    }

    private func decode<T: Decodable>(_ type: T.Type, key: String) -> [T] {
        guard let data = UserDefaults.standard.data(forKey: key),
              let values = try? JSONDecoder().decode([T].self, from: data) else { return [] }
        return values
    }

    private func save() {
        if let data = try? JSONEncoder().encode(tracks) { UserDefaults.standard.set(data, forKey: tracksKey) }
        if let data = try? JSONEncoder().encode(links) { UserDefaults.standard.set(data, forKey: linksKey) }
    }
}

enum MusicLibraryError: Error {
    case invalidLink
}
