import Foundation
import SwiftUI

struct AnimeCustomList: Identifiable, Codable, Hashable {
    var id: UUID
    var name: String
    var animeIDs: Set<Int>

    init(id: UUID = UUID(), name: String, animeIDs: Set<Int> = []) {
        self.id = id
        self.name = name
        self.animeIDs = animeIDs
    }
}

@MainActor
final class CustomListsStore: ObservableObject {
    @Published private(set) var lists: [AnimeCustomList] = []
    private let storageKey = "anilarper.ios.customLists.v1"

    init() {
        if let data = UserDefaults.standard.data(forKey: storageKey),
           let decoded = try? JSONDecoder().decode([AnimeCustomList].self, from: data) {
            lists = decoded
        }
    }

    func create(name: String) {
        let clean = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !clean.isEmpty, !lists.contains(where: { $0.name.caseInsensitiveCompare(clean) == .orderedSame }) else { return }
        lists.append(AnimeCustomList(name: clean))
        save()
    }

    func delete(_ list: AnimeCustomList) {
        lists.removeAll { $0.id == list.id }
        save()
    }

    func toggle(_ anime: Anime, in list: AnimeCustomList) {
        guard let index = lists.firstIndex(where: { $0.id == list.id }) else { return }
        if lists[index].animeIDs.contains(anime.id) {
            lists[index].animeIDs.remove(anime.id)
        } else {
            lists[index].animeIDs.insert(anime.id)
        }
        save()
    }

    private func save() {
        guard let data = try? JSONEncoder().encode(lists) else { return }
        UserDefaults.standard.set(data, forKey: storageKey)
    }
}
