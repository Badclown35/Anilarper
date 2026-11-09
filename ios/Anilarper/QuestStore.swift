import Foundation
import SwiftUI

@MainActor
final class QuestStore: ObservableObject {
    @Published private(set) var experience: Int
    @Published private(set) var claimed: Set<String>
    private let xpKey = "anilarper.ios.questXP"
    private let claimedKey = "anilarper.ios.claimedQuests"

    init() {
        experience = UserDefaults.standard.integer(forKey: xpKey)
        claimed = Set(UserDefaults.standard.stringArray(forKey: claimedKey) ?? [])
    }

    func hasClaimed(_ id: String) -> Bool { claimed.contains(id) }

    func claim(id: String, reward: Int) {
        guard !claimed.contains(id) else { return }
        claimed.insert(id)
        experience += reward
        UserDefaults.standard.set(experience, forKey: xpKey)
        UserDefaults.standard.set(Array(claimed), forKey: claimedKey)
    }
}
