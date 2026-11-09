import Foundation
import SwiftUI

@MainActor
final class ReadingStore: ObservableObject {
    @Published private(set) var titles: [ReadingTitle] = []
    private let storageKey = "anilarper.ios.reading.v1"

    init() {
        if let data = UserDefaults.standard.data(forKey: storageKey),
           let decoded = try? JSONDecoder().decode([ReadingTitle].self, from: data) {
            titles = decoded
        }
    }

    func add(_ title: ReadingTitle) {
        guard !titles.contains(where: { $0.id == title.id && $0.kind == title.kind }) else { return }
        titles.append(title)
        save()
    }

    func update(_ title: ReadingTitle) {
        guard let index = titles.firstIndex(where: { $0.id == title.id && $0.kind == title.kind }) else { return }
        titles[index] = title
        save()
    }

    func importTitles(_ imported: [ReadingTitle]) {
        var merged = Dictionary(uniqueKeysWithValues: titles.map { ("\($0.kind.rawValue)-\($0.id)", $0) })
        for item in imported {
            let key = "\(item.kind.rawValue)-\(item.id)"
            if var existing = merged[key] {
                existing.progress = item.progress
                existing.volumeProgress = item.volumeProgress
                existing.readingStatus = item.readingStatus
                merged[key] = existing
            } else {
                merged[key] = item
            }
        }
        titles = Array(merged.values).sorted { $0.title.localizedCaseInsensitiveCompare($1.title) == .orderedAscending }
        save()
    }

    func remove(_ title: ReadingTitle) {
        titles.removeAll { $0.id == title.id && $0.kind == title.kind }
        save()
    }

    private func save() {
        guard let data = try? JSONEncoder().encode(titles) else { return }
        UserDefaults.standard.set(data, forKey: storageKey)
    }
}
