import Foundation
import SwiftUI

@MainActor
final class LibraryStore: ObservableObject {
    @Published private(set) var anime: [Anime] = []

    private let storageKey = "anilarper.ios.library.v1"

    init() {
        load()
    }

    func contains(_ item: Anime) -> Bool {
        anime.contains { $0.id == item.id }
    }

    func add(_ item: Anime) {
        guard !contains(item) else { return }
        anime.append(item)
        save()
    }

    func importAnime(_ item: Anime) {
        if let index = anime.firstIndex(where: { $0.id == item.id }) {
            var existing = anime[index]
            existing.status = item.status
            existing.progress = item.progress
            existing.score = item.score ?? existing.score
            anime[index] = existing
        } else {
            anime.append(item)
        }
        save()
    }

    func update(_ item: Anime) {
        guard let index = anime.firstIndex(where: { $0.id == item.id }) else { return }
        anime[index] = item
        save()
    }

    func remove(_ item: Anime) {
        anime.removeAll { $0.id == item.id }
        save()
    }

    func replaceAll(with items: [Anime]) {
        var unique: [Int: Anime] = [:]
        for item in items { unique[item.id] = item }
        anime = Array(unique.values).sorted { $0.title.localizedCaseInsensitiveCompare($1.title) == .orderedAscending }
        save()
    }

    func exportData() throws -> Data {
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        return try encoder.encode(anime)
    }

    func importData(_ data: Data) throws {
        let imported = try JSONDecoder().decode([Anime].self, from: data)
        var merged = Dictionary(uniqueKeysWithValues: anime.map { ($0.id, $0) })
        for item in imported {
            if var existing = merged[item.id] {
                existing.status = item.status
                existing.progress = item.progress
                existing.score = item.score ?? existing.score
                merged[item.id] = existing
            } else {
                merged[item.id] = item
            }
        }
        anime = Array(merged.values).sorted { $0.title.localizedCaseInsensitiveCompare($1.title) == .orderedAscending }
        save()
    }

    private func load() {
        guard let data = UserDefaults.standard.data(forKey: storageKey),
              let decoded = try? JSONDecoder().decode([Anime].self, from: data) else { return }
        anime = decoded
    }

    private func save() {
        guard let data = try? JSONEncoder().encode(anime) else { return }
        UserDefaults.standard.set(data, forKey: storageKey)
    }
}
