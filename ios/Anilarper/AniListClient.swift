import Foundation

struct AniListClient {
    private let endpoint = URL(string: "https://graphql.anilist.co")!

    func findAnime(malId: Int) async throws -> AnimeSearchResult? {
        let query = """
        query ($malId: Int) {
          Page(perPage: 1) {
            media(idMal: $malId, type: ANIME) {
              id
              idMal
              title { romaji english native }
              synonyms
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
            "variables": ["malId": malId]
        ])
        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            throw AniListError.invalidResponse
        }
        let decoded = try JSONDecoder().decode(AniListResponse.self, from: data)
        return decoded.data.page.media.first
    }

    func searchReading(_ search: String, kind: ReadingKind) async throws -> [ReadingSearchResult] {
        let query = """
        query ($search: String, $formats: [MediaFormat]) {
          Page(perPage: 20) {
            media(search: $search, type: MANGA, format_in: $formats, isAdult: false,
                  sort: [SEARCH_MATCH]) {
              id
              title { romaji english native }
              synonyms
              coverImage { medium }
              format
              status
              chapters
              volumes
              description(asHtml: false)
            }
          }
        }
        """
        let formats = kind == .novel ? ["NOVEL"] : ["MANGA", "ONE_SHOT"]
        var request = URLRequest(url: endpoint)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: [
            "query": query,
            "variables": ["search": search, "formats": formats]
        ])
        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            throw AniListError.invalidResponse
        }
        return try JSONDecoder().decode(AniListReadingResponse.self, from: data).data.page.media
    }

    func trendingAnime() async throws -> [AnimeSearchResult] {
        let query = """
        query {
          Page(perPage: 20) {
            media(type: ANIME, isAdult: false, sort: [TRENDING_DESC]) {
              id
              idMal
              title { romaji english native }
              synonyms
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
        request.httpBody = try JSONSerialization.data(withJSONObject: ["query": query])
        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            throw AniListError.invalidResponse
        }
        return try JSONDecoder().decode(AniListResponse.self, from: data).data.page.media
    }

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
    let synonyms: [String]?
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


struct ReadingSearchResult: Decodable, Identifiable {
    let id: Int
    let title: AnimeSearchResult.Titles
    let coverImage: AnimeSearchResult.CoverImage?
    let format: String?
    let status: String?
    let chapters: Int?
    let volumes: Int?
    let description: String?
}

private struct AniListReadingResponse: Decodable {
    struct DataPage: Decodable {
        struct Page: Decodable {
            let media: [ReadingSearchResult]
        }
        let page: Page
    }
    let data: DataPage
}
