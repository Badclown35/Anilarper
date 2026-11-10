"""Regression tests for the phone server's public/private boundary."""
import importlib.util
import json
import os
from pathlib import Path
import tempfile
import threading
import unittest
from urllib.error import HTTPError
from urllib.request import Request, urlopen

TEMP = tempfile.TemporaryDirectory(prefix="anilarper-test-")
os.environ["ANILARPER_DATA_DIR"] = str(Path(TEMP.name) / "data")
os.environ["ANILARPER_MEDIA_DIR"] = str(Path(TEMP.name) / "media")
os.environ["ANILARPER_HOST"] = "127.0.0.1"

SERVER_PATH = Path(__file__).with_name("server.py")
SPEC = importlib.util.spec_from_file_location("anilarper_phone_server", SERVER_PATH)
server = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(server)


class PhoneServerTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.httpd = server.ThreadingHTTPServer(("127.0.0.1", 0), server.Handler)
        cls.thread = threading.Thread(target=cls.httpd.serve_forever, daemon=True)
        cls.thread.start()
        cls.base = "http://127.0.0.1:%d" % cls.httpd.server_address[1]

    @classmethod
    def tearDownClass(cls):
        cls.httpd.shutdown()
        cls.httpd.server_close()
        cls.thread.join(timeout=2)
        TEMP.cleanup()

    def setUp(self):
        for path in server.MEDIA.iterdir():
            if path.is_file() or path.is_symlink():
                path.unlink()
        server.write_metadata({
            "public.mp4": {"title": "Public clip", "category": "Clips", "visibility": "public"},
            "private.mp4": {"title": "Private clip", "category": "Clips", "visibility": "private"},
            "legacy.mp4": {"title": "Legacy clip", "category": "Clips"}
        })
        (server.MEDIA / "public.mp4").write_bytes(b"0123456789")
        (server.MEDIA / "private.mp4").write_bytes(b"private")
        (server.MEDIA / "legacy.mp4").write_bytes(b"legacy")

    def request(self, path, method="GET", token=None, body=None, headers=None):
        request_headers = dict(headers or {})
        if token:
            request_headers["Authorization"] = "Bearer " + token
        data = None if body is None else json.dumps(body).encode()
        if data is not None:
            request_headers["Content-Type"] = "application/json"
        request = Request(self.base + path, data=data, headers=request_headers, method=method)
        return urlopen(request)

    def test_public_catalog_hides_private_and_legacy_files(self):
        payload = json.loads(self.request("/api/videos").read())
        self.assertEqual([v["id"] for v in payload["videos"]], ["public.mp4"])
        self.assertIn("storage", payload)

    def test_private_files_cannot_be_streamed_by_guessing_name(self):
        for name in ("private.mp4", "legacy.mp4"):
            with self.assertRaises(HTTPError) as error:
                self.request("/media/" + name)
            self.assertEqual(error.exception.code, 404)

    def test_published_stream_supports_byte_ranges(self):
        response = self.request("/media/public.mp4", headers={"Range": "bytes=2-5"})
        self.assertEqual(response.status, 206)
        self.assertEqual(response.read(), b"2345")
        self.assertEqual(response.headers["Content-Range"], "bytes 2-5/10")

    def test_admin_catalog_requires_token(self):
        with self.assertRaises(HTTPError) as error:
            self.request("/api/admin/videos")
        self.assertEqual(error.exception.code, 401)
        payload = json.loads(self.request("/api/admin/videos", token=server.TOKEN).read())
        self.assertEqual(len(payload["videos"]), 3)

    def test_admin_can_publish_private_file(self):
        response = self.request(
            "/api/videos/private.mp4/metadata", method="POST", token=server.TOKEN,
            body={"title": "Now shared", "category": "Clips", "visibility": "public"})
        self.assertEqual(response.status, 200)
        payload = json.loads(self.request("/api/videos").read())
        ids = [v["id"] for v in payload["videos"]]
        self.assertIn("private.mp4", ids)

    def test_admin_token_is_required_for_mutations(self):
        with self.assertRaises(HTTPError) as error:
            self.request("/api/videos/public.mp4", method="DELETE")
        self.assertEqual(error.exception.code, 401)

    def test_new_upload_is_private_even_if_stale_metadata_was_public(self):
        metadata = server.read_metadata()
        metadata["fresh.mp4"] = {"title": "Old entry", "category": "Clips", "visibility": "public"}
        server.write_metadata(metadata)
        request = Request(
            self.base + "/api/upload?name=fresh.mp4&title=Fresh&category=Clips",
            data=b"new-video", headers={"Authorization": "Bearer " + server.TOKEN},
            method="POST")
        response = urlopen(request)
        self.assertEqual(response.status, 201)
        result = json.loads(response.read())
        self.assertEqual(result["visibility"], "private")
        with self.assertRaises(HTTPError) as error:
            self.request("/media/fresh.mp4")
        self.assertEqual(error.exception.code, 404)

    def test_upload_can_be_explicitly_published(self):
        request = Request(
            self.base + "/api/upload?name=shared.mp4&publish=true",
            data=b"shared-video", headers={"Authorization": "Bearer " + server.TOKEN},
            method="POST")
        response = urlopen(request)
        self.assertEqual(response.status, 201)
        payload = json.loads(self.request("/api/videos").read())
        self.assertIn("shared.mp4", [v["id"] for v in payload["videos"]])


if __name__ == "__main__":
    unittest.main()
