import SwiftUI

struct CustomListsView: View {
    @EnvironmentObject private var library: LibraryStore
    @EnvironmentObject private var customLists: CustomListsStore
    @State private var showingCreate = false
    @State private var newName = ""
    @State private var expandedListID: UUID?

    var body: some View {
        NavigationStack {
            List {
                if customLists.lists.isEmpty {
                    ContentUnavailableView("No custom lists yet", systemImage: "rectangle.stack",
                        description: Text("Create watchlists for favorites, seasonal picks, or anything you want."))
                }
                ForEach(customLists.lists) { list in
                    Section {
                        DisclosureGroup(isExpanded: Binding(
                            get: { expandedListID == list.id },
                            set: { expandedListID = $0 ? list.id : nil }
                        )) {
                            if library.anime.isEmpty {
                                Text("Add anime to your library first.").font(.caption).foregroundStyle(.secondary)
                            } else {
                                ForEach(library.anime.sorted { $0.title < $1.title }) { anime in
                                    Button {
                                        customLists.toggle(anime, in: list)
                                    } label: {
                                        HStack {
                                            Text(anime.title).foregroundStyle(.primary)
                                            Spacer()
                                            Image(systemName: list.animeIDs.contains(anime.id) ? "checkmark.circle.fill" : "circle")
                                                .foregroundStyle(list.animeIDs.contains(anime.id) ? .purple : .secondary)
                                        }
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                        } label: {
                            HStack {
                                VStack(alignment: .leading, spacing: 3) {
                                    Text(list.name).font(.headline)
                                    Text("\(list.animeIDs.count) titles").font(.caption).foregroundStyle(.secondary)
                                }
                                Spacer()
                            }
                        }
                    }
                    .swipeActions {
                        Button(role: .destructive) { customLists.delete(list) } label: {
                            Label("Delete", systemImage: "trash")
                        }
                    }
                }
            }
            .navigationTitle("Custom Lists")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { showingCreate = true } label: { Image(systemName: "plus") }
                }
            }
            .alert("Create a list", isPresented: $showingCreate) {
                TextField("List name", text: $newName)
                Button("Cancel", role: .cancel) { newName = "" }
                Button("Create") {
                    customLists.create(name: newName)
                    newName = ""
                }
            } message: {
                Text("Give your watchlist a name.")
            }
        }
    }
}
