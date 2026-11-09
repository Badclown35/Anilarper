import SwiftUI
import UniformTypeIdentifiers

struct LibraryView: View {
    @EnvironmentObject private var library: LibraryStore
    @State private var filter: AnimeStatus? = nil
    @State private var showingImporter = false
    @State private var showingExporter = false
    @State private var exportDocument = AnimeLibraryDocument()
    @State private var alertMessage: String?

    private var visibleAnime: [Anime] {
        library.anime
            .filter { filter == nil || $0.status == filter }
            .sorted { $0.title.localizedCaseInsensitiveCompare($1.title) == .orderedAscending }
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 12) {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        filterChip("All", selected: filter == nil) { filter = nil }
                        ForEach(AnimeStatus.allCases, id: \.self) { status in
                            filterChip(status.label, selected: filter == status) { filter = status }
                        }
                    }
                    .padding(.horizontal, 16)
                }

                if visibleAnime.isEmpty {
                    ContentUnavailableView("Your library is empty", systemImage: "books.vertical",
                                           description: Text("Use Discover to find anime and add it here."))
                    Spacer()
                } else {
                    List {
                        ForEach(visibleAnime) { item in
                            LibraryAnimeRow(anime: item)
                                .listRowBackground(Color.clear)
                                .swipeActions(edge: .trailing) {
                                    Button(role: .destructive) { library.remove(item) } label: {
                                        Label("Remove", systemImage: "trash")
                                    }
                                }
                        }
                    }
                    .listStyle(.plain)
                }
            }
            .navigationTitle("My Library")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Menu {
                        Button { showingImporter = true } label: { Label("Import JSON backup", systemImage: "square.and.arrow.down") }
                        Button {
                            do {
                                exportDocument = AnimeLibraryDocument(data: try library.exportData())
                                showingExporter = true
                            } catch { alertMessage = "Couldn't prepare your backup." }
                        } label: { Label("Export JSON backup", systemImage: "square.and.arrow.up") }
                    } label: { Image(systemName: "ellipsis.circle") }
                }
            }
            .fileImporter(isPresented: $showingImporter, allowedContentTypes: [.json]) { result in
                do {
                    let url = try result.get()
                    let access = url.startAccessingSecurityScopedResource()
                    defer { if access { url.stopAccessingSecurityScopedResource() } }
                    try library.importData(Data(contentsOf: url))
                    alertMessage = "Backup imported and merged with your library."
                } catch { alertMessage = "Import failed. Make sure this is a valid Anilarper JSON backup." }
            }
            .fileExporter(isPresented: $showingExporter, document: exportDocument,
                          contentType: .json, defaultFilename: "anilarper-library") { result in
                if case .failure = result { alertMessage = "Couldn't export your backup." }
            }
            .alert("Anilarper", isPresented: Binding(get: { alertMessage != nil }, set: { if !$0 { alertMessage = nil } })) {
                Button("OK") { alertMessage = nil }
            } message: { Text(alertMessage ?? "") }
        }
    }

    private func filterChip(_ title: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title).font(.caption.weight(.semibold))
                .padding(.horizontal, 13).padding(.vertical, 8)
                .background(selected ? Color.purple : Color.white.opacity(0.08), in: Capsule())
                .foregroundStyle(.white)
        }
        .buttonStyle(.plain)
    }
}

private struct LibraryAnimeRow: View {
    @EnvironmentObject private var library: LibraryStore
    @State var anime: Anime

    var body: some View {
        HStack(spacing: 12) {
            AsyncImage(url: anime.coverImageURL) { phase in
                if let image = phase.image { image.resizable().scaledToFill() }
                else { RoundedRectangle(cornerRadius: 9).fill(.white.opacity(0.08)) }
            }
            .frame(width: 62, height: 88).clipShape(RoundedRectangle(cornerRadius: 9))
            VStack(alignment: .leading, spacing: 5) {
                Text(anime.title).font(.subheadline.weight(.semibold)).lineLimit(2)
                Text(anime.status.label).font(.caption).foregroundStyle(.purple)
                if let episodes = anime.episodes {
                    Text("Episode \(anime.progress) / \(episodes)").font(.caption2).foregroundStyle(.secondary)
                } else {
                    Text("Episode \(anime.progress)").font(.caption2).foregroundStyle(.secondary)
                }
                Menu {
                    ForEach(AnimeStatus.allCases, id: \.self) { status in
                        Button(status.label) {
                            anime.status = status
                            if status == .completed, let total = anime.episodes { anime.progress = total }
                            library.update(anime)
                        }
                    }
                } label: {
                    Label("Change status", systemImage: "slider.horizontal.3").font(.caption2)
                }
            }
            Spacer()
            Stepper(value: Binding(get: { anime.progress }, set: { anime.progress = max(0, $0); library.update(anime) }),
                    in: 0...(anime.episodes ?? max(anime.progress + 1, 999))) {
                Text("\(anime.progress)").font(.caption.monospacedDigit())
            }
            .labelsHidden()
            .frame(width: 90)
        }
        .padding(.vertical, 5)
    }
}

private extension AnimeStatus {
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

struct AnimeLibraryDocument: FileDocument {
    static var readableContentTypes: [UTType] { [.json] }
    var data: Data

    init(data: Data = Data("[]".utf8)) { self.data = data }
    init(configuration: ReadConfiguration) throws {
        data = configuration.file.regularFileContents ?? Data("[]".utf8)
    }
    func fileWrapper(configuration: WriteConfiguration) throws {
        FileWrapper(regularFileWithContents: data)
    }
}
