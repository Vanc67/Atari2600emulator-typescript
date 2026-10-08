import os
import json
import urllib.request
import urllib.parse
import uuid

def upload_to_gofile(filepath):
    # Step 1: Get best server or direct upload
    server = "store1"
    try:
        req = urllib.request.Request("https://api.gofile.io/servers", headers={"User-Agent": "Mozilla/5.0"})
        with urllib.request.urlopen(req) as resp:
            data = json.loads(resp.read().decode())
            if data.get("status") == "ok" and "servers" in data["data"]:
                server = data["data"]["servers"][0]["name"]
                print(f"Got server: {server}")
    except Exception as e:
        print(f"Failed to get server list, falling back: {e}")

    boundary = "----WebKitFormBoundary" + uuid.uuid4().hex

    with open(filepath, "rb") as f:
        file_bytes = f.read()

    filename = os.path.basename(filepath)

    body = []
    body.append(f"--{boundary}".encode('utf-8'))
    body.append(f'Content-Disposition: form-data; name="file"; filename="{filename}"'.encode('utf-8'))
    body.append(b'Content-Type: application/zip')
    body.append(b'')
    body.append(file_bytes)
    body.append(f"--{boundary}--".encode('utf-8'))
    body.append(b'')

    payload = b"\r\n".join(body)

    urls_to_try = [
        "https://upload.gofile.io/uploadFile",
        f"https://{server}.gofile.io/contents/upload"
    ]

    for url in urls_to_try:
        try:
            print(f"Attempting upload to {url}...")
            req = urllib.request.Request(
                url,
                data=payload,
                headers={
                    "Content-Type": f"multipart/form-data; boundary={boundary}",
                    "User-Agent": "Mozilla/5.0"
                },
                method="POST"
            )
            with urllib.request.urlopen(req) as response:
                res_text = response.read().decode('utf-8')
                print(f"Response: {res_text}")
                res_json = json.loads(res_text)
                if res_json.get("status") == "ok":
                    download_page = res_json["data"]["downloadPage"]
                    print(f"SUCCESS! Download Page: {download_page}")
                    return download_page
        except Exception as e:
            print(f"Error uploading to {url}: {e}")

    return None

if __name__ == "__main__":
    link = upload_to_gofile("LineageOS-22.0-zeroflte-UNOFFICIAL.zip")
    print("FINAL LINK:", link)
