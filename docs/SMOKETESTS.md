Authentication

☐ https://luvd1s.onrender.com/ redirects to /login when logged out
☐ admin / admin123 logs in successfully
☐ Log out works and returns to /login
☐ /register creates a new account
☐ Logging in as the new account shows an empty People grid

People CRUD

☐ Grid loads with admin's 5 seeded contacts
☐ Select a contact → form populates
☐ Change the first name → Save button enables
☐ Save → green pulse, grid fades, notification appears
☐ Clear the first name → Save → form shakes, validation fires
☐ New → blank form, Save disabled
☐ Delete → confirm dialog → contact disappears

Tags

☐ Tag filter dropdown shows 4 tags
☐ Selecting "work" filters the grid to 4 contacts
☐ Clearing the filter restores all 5
☐ Add/remove a tag on a contact → chips update

Interactions

☐ Lightning icon in the grid opens the log dialog
☐ Submitting with no type → form shakes
☐ Save → interaction appears on the person's detail page
☐ Person detail shows the interaction with correct timestamp
☐ Edit icon → pre-populated dialog → update works
☐ Trash icon → confirm → card disappears

Charts

☐ Dashboard line chart has visible data points
☐ Interaction types bar chart shows non-zero bars
☐ Person detail shows the same charts scoped to that person
☐ Charts render in dark mode without visibility loss

Theme

☐ Moon icon → page flips to dark
☐ Refresh → still dark, no white flash
☐ Navigate to /dashboard → still dark
☐ Tap sun icon → back to light
☐ Log out → login page honors theme

AI chat

☐ FAB appears bottom-right on People, Dashboard, and Person detail
☐ Speech bubble appears after ~1.2s on first load
☐ Tapping FAB or bubble opens the dialog
☐ Bubble doesn't reappear on navigation or refresh
☐ "Who should I reach out to?" returns a reply within ~5s
☐ Free-text message gets a reply
☐ Closing browser fully, reopening → bubble returns

Debug (/debug, admin only)

☐ Non-admin user gets 403 or redirect
☐ Boot animation plays on first visit of the session
☐ Refresh → boot skipped
☐ Recent requests card shows live traffic
☐ Audit trail shows recent writes
☐ Business counters increment after CRUD actions
☐ CRT toggle cycles Off → Subtle → Full
☐ AI card shows provider configured: yes

API

```bash
KEY="lvd_..."

# Should return 200 + JSON
curl -s -H "Authorization: Bearer $KEY" https://luvd1s.onrender.com/api/people | head -c 200

# Should return 401, not 302
curl -i https://luvd1s.onrender.com/api/people | head -3

# Should return 201
curl -i -X POST -H "Authorization: Bearer $KEY" -H "Content-Type: application/json" \
  -d '{"type":"call","summary":"Test"}' \
  https://luvd1s.onrender.com/api/people/1/interactions | head -3

# Actuator health, no auth
curl -s https://luvd1s.onrender.com/actuator/health
```

Persistence

☐ Create a new contact
☐ Push any commit to force a redeploy
☐ Wait for green
☐ Refresh → the new contact is still there
