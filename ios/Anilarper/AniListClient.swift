import Foundation

struct AniListClient {
    private let endpoint = URL(string: "https://graphql.anilist.co")!

    func searchAnime(_ search: String) async throws -> [AnimeSearchResult] {
        let query = """
        query ($search: String) {
          Page(perPage: 20) {
            media(search: $search, type: ANIME) {
              id
              idMal
              title { romaji english native }
              coverImage { medium }
              episodes
              averageScore
            }
          }
        }
        """
        var request = URLRequest(url: endpoint)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: [
            "query": query,
            "variables": ["search": search]
        ])

        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            throw AniListError.invalidResponse
        }
        let decoded = try JSONDecoder().decode(AniListResponse.self, from: data)
        return decoded.data.page.media
    }
}

struct AnimeSearchResult: Decodable, Identifiable {
    struct Titles: Decodable {
        let romaji: String?
        let english: String?
        let native: String?
    }

    struct CoverImage: Decodable {
        let medium: URL?
    }

    let id: Int
    let idMal: Int?
    let title: Titles
    let coverImage: CoverImage?
    let episodes: Int?
    let averageScore: Int?

    var displayTitle: String {
        title.english ?? title.romaji ?? title.native ?? "Unknown title"
    }
}

private struct AniListResponse: Decodable {
    struct DataPage: Decodable {
        struct Page: Decodable {
            let media: [AnimeSearchResult]
        }
        let page: Page
    }
    let data: DataPage
}

enum AniListError: Error {
    case invalidResponse
}
