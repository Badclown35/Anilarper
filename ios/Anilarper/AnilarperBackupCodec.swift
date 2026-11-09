import Foundation

struct ImportedAnilarperBackup {
    let anime: [Anime]
    let reading: [ReadingTitle]
}

enum AnilarperBackupCodec {
    static func encode(anime: [Anime], reading: [ReadingTitle]) throws -> Data {
        let animeRows: [[String: Any]] = anime.map { item in
            var row: [String: Any] = [
                "id": item.id,
                "title": item.title,
                "nativeTitle": item.nativeTitle ?? "",
                "cover": item.coverImageURL?.absoluteString ?? "",
                "format": item.format ?? "",
                "status": item.airingStatus ?? "",
                "description": item.description ?? "",
                "watchStatus": item.status.rawValue.uppercased(),
                "progress": item.progress,
                "lastWatchedAt": item.lastWatchedAt ?? 0,
                "season": item.season ?? "",
                "genres": item.genres ?? [],
                "studios": item.studios ?? []
            ]
            row["malId"] = item.malId.map { $0 as Any } ?? NSNull()
            row["episodes"] = item.episodes.map { $0 as Any } ?? NSNull()
            row["score"] = item.score.map { $0 as Any } ?? NSNull()
            row["seasonYear"] = item.seasonYear.map { $0 as Any } ?? NSNull()
            return row
        }

        let readingRows: [[String: Any]] = reading.map { item in
            var row: [String: Any] = [
                "id": item.id,
                "title": item.title,
                "nativeTitle": item.nativeTitle ?? "",
                "cover": item.coverImageURL?.absoluteString ?? "",
                "format": item.format,
                "status": item.status,
                "description": item.description,
                "kind": item.kind == .novel ? "NOVEL" : "MANGA",
                "progress": item.progress,
                "volumeProgress": item.volumeProgress,
                "readingStatus": item.readingStatus.rawValue.uppercased()
            ]
            row["chapters"] = item.chapters.map { $0 as Any } ?? NSNull()
            row["volumes"] = item.volumes.map { $0 as Any } ?? NSNull()
            return row
        }

        let preferences: [String: Any] = [
            "library": animeRows,
            "readingLibrary": readingRows
        ]
        let root: [String: Any] = [
            "format": "anilarper-backup",
            "version": 1,
            "preferences": preferences
        ]
        return try JSONSerialization.data(withJSONObject: root, options: [.prettyPrinted, .sortedKeys])
    }

    static func decode(_ data: Data) throws -> ImportedAnilarperBackup {
        if let legacy = try? JSONDecoder().decode([Anime].self, from: data) {
            return ImportedAnilarperBackup(anime: legacy, reading: [])
        }

        guard let root = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            throw BackupCodecError.invalidFormat
        }
        let preferences = root["preferences"] as? [String: Any] ?? root
        let animeRows = preferences["library"] as? [[String: Any]] ?? []
        let readingRows = preferences["readingLibrary"] as? [[String: Any]] ?? []

        let anime = animeRows.compactMap(decodeAnime)
        let reading = readingRows.compactMap(decodeReading)
        guard !animeRows.isEmpty || !readingRows.isEmpty else { throw BackupCodecError.invalidFormat }
        return ImportedAnilarperBackup(anime: anime, reading: reading)
    }

    private static func decodeAnime(_ row: [String: Any]) -> Anime? {
        guard let id = integer(row["id"]), let title = row["title"] as? String else { return nil }
        let rawStatus = (row["watchStatus"] as? String ?? "PLANNING").uppercased()
        let status = AnimeStatus(rawValue: rawStatus.lowercased()) ?? .planning
        return Anime(
            id: id,
            malId: integer(row["malId"]),
            title: title,
            nativeTitle: row["nativeTitle"] as? String,
            coverImageURL: (row["cover"] as? String).flatMap(URL.init(string:)),
            episodes: integer(row["episodes"]),
            score: number(row["score"]),
            status: status,
            progress: integer(row["progress"]) ?? 0,
            format: row["format"] as? String,
            airingStatus: row["status"] as? String,
            description: row["description"] as? String,
            season: row["season"] as? String,
            seasonYear: integer(row["seasonYear"]),
            genres: row["genres"] as? [String],
            studios: row["studios"] as? [String],
            lastWatchedAt: integer64(row["lastWatchedAt"])
        )
    }

    private static func decodeReading(_ row: [String: Any]) -> ReadingTitle? {
        guard let id = integer(row["id"]), let title = row["title"] as? String else { return nil }
        let kind = (row["kind"] as? String)?.uppercased() == "NOVEL" ? ReadingKind.novel : .manga
        let rawStatus = (row["readingStatus"] as? String ?? "PLANNING").lowercased()
        let readingStatus = ReadingStatus(rawValue: rawStatus) ?? .planning
        return ReadingTitle(
            id: id,
            title: title,
            nativeTitle: row["nativeTitle"] as? String,
            coverImageURL: (row["cover"] as? String).flatMap(URL.init(string:)),
            format: row["format"] as? String ?? "",
            status: row["status"] as? String ?? "",
            chapters: integer(row["chapters"]),
            volumes: integer(row["volumes"]),
            description: row["description"] as? String ?? "",
            kind: kind,
            progress: integer(row["progress"]) ?? 0,
            volumeProgress: integer(row["volumeProgress"]) ?? 0,
            readingStatus: readingStatus
        )
    }

    private static func integer(_ value: Any?) -> Int? {
        guard let number = value as? NSNumber else { return nil }
        return number.intValue
    }

    private static func integer64(_ value: Any?) -> Int64? {
        guard let number = value as? NSNumber else { return nil }
        return number.int64Value
    }

    private static func number(_ value: Any?) -> Double? {
        guard let number = value as? NSNumber else { return nil }
        return number.doubleValue
    }
}

enum BackupCodecError: Error {
    case invalidFormat
}
