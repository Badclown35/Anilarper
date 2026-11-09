import Foundation

struct MALAnimeEntry {
    let malId: Int
    let title: String
    let status: String
    let watchedEpisodes: Int
    let score: Int
}

enum MALImportError: Error {
    case invalidXML
}

final class MALXMLParser: NSObject, XMLParserDelegate {
    private var entries: [MALAnimeEntry] = []
    private var currentEntry: [String: String]?
    private var currentElement: String?
    private var parserFailed = false

    static func parse(_ data: Data) throws -> [MALAnimeEntry] {
        let delegate = MALXMLParser()
        let parser = XMLParser(data: data)
        parser.delegate = delegate
        guard parser.parse(), !delegate.parserFailed else { throw MALImportError.invalidXML }
        return delegate.entries
    }

    func parser(_ parser: XMLParser, didStartElement elementName: String,
                namespaceURI: String?, qualifiedName qName: String?,
                attributes attributeDict: [String: String] = [:]) {
        if elementName == "anime" { currentEntry = [:] }
        currentElement = elementName
    }

    func parser(_ parser: XMLParser, foundCharacters string: String) {
        guard let element = currentElement, currentEntry != nil else { return }
        currentEntry?[element, default: ""] += string
    }

    func parser(_ parser: XMLParser, didEndElement elementName: String,
                namespaceURI: String?, qualifiedName qName: String?) {
        if elementName == "anime", let entry = currentEntry {
            let id = Int(entry["series_animedb_id"] ?? "") ?? 0
            let title = (entry["series_title"] ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
            if id > 0, !title.isEmpty {
                entries.append(MALAnimeEntry(
                    malId: id,
                    title: title,
                    status: entry["my_status"] ?? "Plan to Watch",
                    watchedEpisodes: Int(entry["my_watched_episodes"] ?? "") ?? 0,
                    score: Int(entry["my_score"] ?? "") ?? 0
                ))
            }
            currentEntry = nil
        }
        if currentElement == elementName { currentElement = nil }
    }

    func parser(_ parser: XMLParser, parseErrorOccurred parseError: Error) {
        parserFailed = true
    }
}
