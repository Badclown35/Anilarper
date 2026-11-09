import SwiftUI

private struct QuestDefinition: Identifiable {
    let id: String
    let title: String
    let detail: String
    let progress: Int
    let target: Int
    let reward: Int
}

struct QuestsView: View {
    @EnvironmentObject private var library: LibraryStore
    @EnvironmentObject private var reading: ReadingStore
    @EnvironmentObject private var quests: QuestStore

    private var definitions: [QuestDefinition] {
        [
            QuestDefinition(id: "first_save", title: "First addition",
                detail: "Save an anime to your library.", progress: library.anime.isEmpty ? 0 : 1, target: 1, reward: 25),
            QuestDefinition(id: "episode_one", title: "Track your progress",
                detail: "Log an episode watched.", progress: library.anime.reduce(0) { $0 + $1.progress } > 0 ? 1 : 0, target: 1, reward: 30),
            QuestDefinition(id: "reading_one", title: "A new chapter",
                detail: "Add manga or a light novel to your shelf.", progress: reading.titles.isEmpty ? 0 : 1, target: 1, reward: 25),
            QuestDefinition(id: "collection_three", title: "Building a collection",
                detail: "Save three anime titles.", progress: min(library.anime.count, 3), target: 3, reward: 50),
            QuestDefinition(id: "finish_one", title: "One story completed",
                detail: "Mark an anime as completed.", progress: library.anime.contains { $0.status == .completed && $0.progress > 0 } ? 1 : 0, target: 1, reward: 60)
        ]
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    VStack(alignment: .leading, spacing: 5) {
                        Text("YOUR JOURNEY").font(.caption.weight(.bold)).tracking(1.8).foregroundStyle(.purple)
                        Text("Quests & milestones").font(.largeTitle.bold())
                        Text("Optional goals. No streaks, deadlines, or penalties for taking a break.")
                            .font(.caption).foregroundStyle(.secondary)
                    }
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("TOTAL EXPERIENCE").font(.caption2.weight(.bold)).tracking(1.2).foregroundStyle(.purple)
                            Text("\(quests.experience) XP").font(.largeTitle.bold().monospacedDigit())
                            Text("Earn XP when you choose to claim a completed milestone.")
                                .font(.caption2).foregroundStyle(.secondary)
                        }
                        Spacer()
                        Image(systemName: "trophy.fill").font(.system(size: 38)).foregroundStyle(.purple)
                    }
                    .padding(18)
                    .background(LinearGradient(colors: [Color.purple.opacity(0.3), Color.white.opacity(0.06)],
                                               startPoint: .topLeading, endPoint: .bottomTrailing),
                                in: RoundedRectangle(cornerRadius: 18))
                    ForEach(definitions) { quest in
                        let complete = quest.progress >= quest.target
                        let claimed = quests.hasClaimed(quest.id)
                        VStack(alignment: .leading, spacing: 10) {
                            HStack(alignment: .top) {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(quest.title).font(.headline)
                                    Text(quest.detail).font(.caption).foregroundStyle(.secondary)
                                }
                                Spacer()
                                Text("+\(quest.reward) XP").font(.caption.weight(.bold)).foregroundStyle(.purple)
                            }
                            ProgressView(value: Double(quest.progress), total: Double(quest.target)).tint(.purple)
                            HStack {
                                Text("\(quest.progress)/\(quest.target)").font(.caption2).foregroundStyle(.secondary)
                                Spacer()
                                if claimed {
                                    Label("Claimed", systemImage: "checkmark.circle.fill")
                                        .font(.caption2.weight(.bold)).foregroundStyle(.secondary)
                                } else {
                                    Button("Claim reward") {
                                        quests.claim(id: quest.id, reward: quest.reward)
                                    }
                                    .font(.caption.weight(.semibold))
                                    .buttonStyle(.borderedProminent)
                                    .tint(.purple)
                                    .disabled(!complete)
                                }
                            }
                        }
                        .padding(15)
                        .background(.white.opacity(0.06), in: RoundedRectangle(cornerRadius: 15))
                    }
                }
                .padding(18)
            }
            .navigationTitle("Quests")
        }
    }
}
