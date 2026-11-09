import Foundation
import AVFoundation

@MainActor
final class MusicPlayerModel: ObservableObject {
    @Published private(set) var currentTrackID: UUID?
    @Published private(set) var isPlaying = false
    @Published private(set) var message: String?
    private var player: AVPlayer?

    func play(_ track: LocalAudioTrack, url: URL) {
        do {
            player?.pause()
            let item = AVPlayerItem(url: url)
            player = AVPlayer(playerItem: item)
            currentTrackID = track.id
            player?.play()
            isPlaying = true
            message = nil
        }
    }

    func toggle() {
        guard let player else { return }
        if isPlaying {
            player.pause()
            isPlaying = false
        } else {
            player.play()
            isPlaying = true
        }
    }

    func stop() {
        player?.pause()
        player = nil
        currentTrackID = nil
        isPlaying = false
    }
}
