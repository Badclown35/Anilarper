import SwiftUI

struct ReadingShelfView: View {
    @EnvironmentObject private var reading: ReadingStore
    @State private var kind: ReadingKind = .manga
    @State private var query = ""
    @State private var results: [ReadingSearchResult] = []
    @State private var isSearching = false
    @State private var searched = false
    @State private var errorMessage: String?
    private let client = AniListClient()

    private var saved: [ReadingTitle] {
        reading.titles.filter { $0.kind == kind }
            .sorted { $0.title.localizedCaseInsensitiveCompare($1.title) == .orderedAscending }
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 12) {
                Picker("Shelf", selection: $kind) {
                    ForEach(ReadingKind.allCases, id: \.self) { item in
                        Text(item.label).tag(item)
                    }
                }
                .pickerStyle(.segmented)
                .onChange(of: kind) { _, _ in results = []; searched = false; errorMessage = nil }

                HStack(spacing: 9) {
                    Image(systemName: "magnifyingglass").foregroundStyle(.secondary)
                    TextField(kind == .manga ? "Search manga" : "Search light novels", text: $query)
                        .submitLabel(.search)
                        .onSubmit { Task { await search() } }
                    Button("Search") { Task { await search() } }
                        .font(.caption.weight(.semibold))
                        .disabled(query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isSearching)
                }
                .padding(12)
                .background(.white.opacity(0.07), in: RoundedRectangle(cornerRadius: 13))

                if isSearching {
                    ProgressView("Searching AniList…").tint(.purple)
                } else if let errorMessage {
                    Text(errorMessage).font(.caption).foregroundStyle(.secondary)
                }

                List {
                    if !saved.isEmpty {
                        Section("YOUR SHELF") {
                            ForEach(saved) { title in
                                ReadingShelfRow(title: title)
                                    .listRowBackground(Color.clear)
                                    .swipeActions {
                                        Button(role: .destructive) { reading.remove(title) } label: {
                                            Label("Remove", systemImage: "trash")
                                        }
                                    }
                            }
                        }
                    }
                    if !results.isEmpty {
                        Section("DISCOVER") {
                            ForEach(results) { result in
                                ReadingResultRow(result: result,
                                    isSaved: reading.titles.contains { $0.id == result.id && $0.kind == kind }) {
                                    reading.add(result.asReading(kind: kind))
                                }
                                .listRowBackground(Color.clear)
                            }
                        }
                    }
                    if saved.isEmpty && results.isEmpty && !isSearching {
                        ContentUnavailableView(searched ? "No titles found" : "Your reading shelf",
                            systemImage: "book.closed",
                            description: Text(searched ? "Try a different search." : "Search AniList to add manga and light novels."))
                    }
                }
                .listStyle(.insetGrouped)
            }
            .padding(.horizontal, 14)
            .padding(.top, 10)
            .navigationTitle("Reading")
        }
    }

    @MainActor
    private func search() async {
        let term = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !term.isEmpty else { return }
        isSearching = true
        errorMessage = nil
        searched = true
        defer { isSearching = false }
        do {
            results = try await client.searchReading(term, kind: kind)
        } catch {
            errorMessage = "Couldn't reach AniList. Check your connection and try again."
        }
    }
}

private struct ReadingResultRow: View {
    let result: ReadingSearchResult
    let isSaved: Bool
    let onAdd: () -> Void

    var body: some View {
        HStack(spacing: 11) {
            AsyncImage(url: result.coverImage?.medium) { phase in
                if let image = phase.image { image.resizable().scaledToFill() }
                else { RoundedRectangle(cornerRadius: 8).fill(.white.opacity(0.08)) }
            }
            .frame(width: 55, height: 76).clipShape(RoundedRectangle(cornerRadius: 8))
            VStack(alignment: .leading, spacing: 5) {
                Text(result.title.english ?? result.title.romaji ?? result.title.native ?? "Unknown title")
                    .font(.subheadline.weight(.semibold)).lineLimit(2)
                Text([result.format, result.chapters.map { "\($0) chapters" }, result.volumes.map { "\($0) volumes" }]
                    .compactMap { $0 }.joined(separator: " · "))
                    .font(.caption2).foregroundStyle(.secondary)
                Button(action: onAdd) {
                    Label(isSaved ? "On shelf" : "Add to shelf",
                          systemImage: isSaved ? "checkmark.circle.fill" : "plus.circle.fill")
                        .font(.caption.weight(.semibold))
                }
                .buttonStyle(.borderedProminent).tint(isSaved ? .gray : .purple).disabled(isSaved)
            }
            Spacer(minLength: 0)
        }
        .padding(.vertical, 4)
    }
}

private struct ReadingShelfRow: View {
    @EnvironmentObject private var reading: ReadingStore
    @State var title: ReadingTitle

    var body: some View {
        HStack(spacing: 10) {
            AsyncImage(url: title.coverImageURL) { phase in
                if let image = phase.image { image.resizable().scaledToFill() }
                else { RoundedRectangle(cornerRadius: 8).fill(.white.opacity(0.08)) }
            }
            .frame(width: 54, height: 74).clipShape(RoundedRectangle(cornerRadius: 8))
            VStack(alignment: .leading, spacing: 5) {
                Text(title.title).font(.subheadline.weight(.semibold)).lineLimit(2)
                Text(title.readingStatus.label).font(.caption).foregroundStyle(.purple)
                Text("\(title.progress)/\(title.chapters.map(String.init) ?? "?") chapters")
                    .font(.caption2).foregroundStyle(.secondary)
                HStack(spacing: 12) {
                    Stepper("Ch. \(title.progress)", value: Binding(
                        get: { title.progress },
                        set: { title.progress = max(0, $0); reading.update(title) }
                    ), in: 0...max(title.chapters ?? 999, title.progress))
                    .font(.caption2)
                }
                Menu {
                    ForEach(ReadingStatus.allCases, id: \.self) { status in
                        Button(status.label) {
                            title.readingStatus = status
                            if status == .completed, let total = title.chapters { title.progress = total }
                            reading.update(title)
                        }
                    }
                } label: {
                    Label("Status", systemImage: "slider.horizontal.3").font(.caption2)
                }
            }
        }
        .padding(.vertical, 4)
    }
}

private extension ReadingSearchResult {
    func asReading(kind: ReadingKind) -> ReadingTitle {
        ReadingTitle(id: id,
            title: title.english ?? title.romaji ?? title.native ?? "Unknown title",
            nativeTitle: title.native,
            coverImageURL: coverImage?.medium,
            format: format ?? (kind == .novel ? "NOVEL" : "MANGA"),
            status: status ?? "UNKNOWN",
            chapters: chapters, volumes: volumes,
            description: description ?? "",
            kind: kind, progress: 0, volumeProgress: 0, readingStatus: .planning)
    }
}
