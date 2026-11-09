import SwiftUI
import UniformTypeIdentifiers

struct MusicView: View {
    @EnvironmentObject private var music: MusicLibraryStore
    @StateObject private var player = MusicPlayerModel()
    @State private var showingFilePicker = false
    @State private var showingLinkForm = false
    @State private var linkTitle = ""
    @State private var linkURL = ""
    @State private var message: String?

    var body: some View {
        NavigationStack {
            List {
                Section {
                    HStack(spacing: 10) {
                        Button { showingFilePicker = true } label: {
                            Label("Add audio", systemImage: "plus")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.borderedProminent).tint(.purple)
                        Button { showingLinkForm = true } label: {
                            Label("Add link", systemImage: "link")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.bordered)
                    }
                    if let message {
                        Text(message).font(.caption).foregroundStyle(.secondary)
                    }
                }

                if let currentID = player.currentTrackID,
                   let current = music.tracks.first(where: { $0.id == currentID }) {
                    Section("NOW PLAYING") {
                        HStack {
                            Image(systemName: "waveform").font(.title2).foregroundStyle(.purple)
                            VStack(alignment: .leading, spacing: 3) {
                                Text(current.title).font(.subheadline.weight(.semibold))
                                Text(player.isPlaying ? "Playing" : "Paused").font(.caption).foregroundStyle(.secondary)
                            }
                            Spacer()
                            Button { player.toggle() } label: {
                                Image(systemName: player.isPlaying ? "pause.fill" : "play.fill")
                                    .font(.title2).foregroundStyle(.purple)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }

                Section("LOCAL AUDIO") {
                    if music.tracks.isEmpty {
                        Text("Import audio files stored on your device.").font(.caption).foregroundStyle(.secondary)
                    }
                    ForEach(music.tracks) { track in
                        HStack(spacing: 10) {
                            Button {
                                if player.currentTrackID == track.id {
                                    player.toggle()
                                } else {
                                    do { player.play(track, url: try music.fileURL(for: track)) }
                                    catch { message = "Couldn't open this audio file." }
                                }
                            } label: {
                                Image(systemName: player.currentTrackID == track.id && player.isPlaying ? "pause.circle.fill" : "play.circle.fill")
                                    .font(.title2).foregroundStyle(.purple)
                            }
                            .buttonStyle(.plain)
                            Text(track.title).font(.subheadline).lineLimit(2)
                            Spacer()
                            Button(role: .destructive) {
                                if player.currentTrackID == track.id { player.stop() }
                                music.remove(track)
                            } label: { Image(systemName: "trash").foregroundStyle(.secondary) }
                                .buttonStyle(.plain)
                        }
                    }
                }

                Section("MUSIC LINKS") {
                    if music.links.isEmpty {
                        Text("Save links to music services or playlists you use.").font(.caption).foregroundStyle(.secondary)
                    }
                    ForEach(music.links) { link in
                        HStack {
                            Link(destination: URL(string: link.url)!) {
                                Label(link.title, systemImage: "arrow.up.right.square")
                                    .lineLimit(2)
                            }
                            Spacer()
                            Button(role: .destructive) { music.remove(link) } label: {
                                Image(systemName: "trash").foregroundStyle(.secondary)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }
            .navigationTitle("Soundtrack")
            .fileImporter(isPresented: $showingFilePicker, allowedContentTypes: [.audio]) { result in
                do {
                    let url = try result.get()
                    let access = url.startAccessingSecurityScopedResource()
                    defer { if access { url.stopAccessingSecurityScopedResource() } }
                    try music.importAudio(from: url)
                    message = "Audio added to your local soundtrack."
                } catch {
                    message = "Couldn't import that audio file."
                }
            }
            .sheet(isPresented: $showingLinkForm) {
                NavigationStack {
                    Form {
                        TextField("Link title", text: $linkTitle)
                        TextField("https://…", text: $linkURL).keyboardType(.URL).textInputAutocapitalization(.never)
                    }
                    .navigationTitle("Add music link")
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) {
                            Button("Cancel") { showingLinkForm = false }
                        }
                        ToolbarItem(placement: .confirmationAction) {
                            Button("Save") {
                                do {
                                    try music.addLink(title: linkTitle, url: linkURL)
                                    linkTitle = ""; linkURL = ""; showingLinkForm = false
                                    message = "Music link saved."
                                } catch { message = "Enter a title and a valid web link." }
                            }
                        }
                    }
                }
            }
        }
    }
}
