"""
Usage:
  python tools/send_test.py <id> [case]

Cases:
  normal     - full valid payload (default)
  nobanner   - bannerImg missing
  badimage   - bannerImg is a broken URL
  badtime    - timestamp is not a number
  notitle    - title and description missing
"""
import sys
import time
import firebase_admin
from firebase_admin import credentials, messaging

TOKEN = "eBizAGpeR566r0wKYqZEYw:APA91bGfe1au4GD1eiMgc2q2qSKyZd3P247mrqm6gOIH8KEoiL0kNF7oew-UOVDUTIjJT2Avw_qh6xXgLXD6JYsleWdFqeWVTILiRtxRcVniyFn0SxyUkIs"

cred = credentials.Certificate("tools/service-account.json")
firebase_admin.initialize_app(cred)

notif_id = sys.argv[1] if len(sys.argv) > 1 else f"notification_{int(time.time())}"
case = sys.argv[2] if len(sys.argv) > 2 else "normal"

data = {
    "id": notif_id,
    "title": "New Notification",
    "description": "This is a sample notification for the assignment.",
    "bannerImg": "https://picsum.photos/800/400",
    "timestamp": str(int(time.time() * 1000)),
}

if case == "nobanner":
    data.pop("bannerImg")
elif case == "badimage":
    data["bannerImg"] = "https://example.com/does-not-exist.jpg"
elif case == "badtime":
    data["timestamp"] = "abc"
elif case == "notitle":
    data.pop("title")
    data.pop("description")

message = messaging.Message(
    token=TOKEN,
    data=data,
    android=messaging.AndroidConfig(priority="high"),
)
print(f"[{case}] Sent:", messaging.send(message))