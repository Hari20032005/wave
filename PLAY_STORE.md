# Play Console — submission pack (M6)

## 1. Data safety form answers

**Does your app collect or share any of the required user data types?** → **No.**

Rationale you can defend in review:
- All behavioral data is stored on-device only and never transmitted (the app
  has no INTERNET permission — reviewers can verify in the manifest).
- "Collected" per Play's definition means transmitted off the device — nothing is.
- Purchases are processed by Google Play Billing; Play is the data controller
  for payment data.

Additional questions:
- Data encrypted in transit? → No data in transit.
- Deletion mechanism? → Yes: in-app "Delete all my data" (Settings).

## 2. Content rating questionnaire (IARC)

- Category: Utility / Productivity / Health app.
- Violence, sexuality, language, controlled substances: answer **No** to all —
  the app contains no sexual content or imagery anywhere; it references
  "pornography" only as a clinical word for a habit users want to manage.
- "Does the app provide health or medical information/advice?" — answer
  conservatively: it is a wellbeing/habit tool; it makes no medical claims and
  offers no diagnosis or treatment.
- Target age: set **17+** in the questionnaire and in ads settings.

## 3. Store listing (digital-wellbeing positioning — no explicit terms)

**App name:** Stillwater — Digital Wellbeing & Focus

**Short description (80 chars):**
"A calm companion for urges and impulse control. Private: nothing leaves your phone."

**Full description (draft):**
Stillwater helps you change your relationship with the apps and habits that
pull at you. Instead of streaks and shame, it gives you help in the moment the
urge rises:

- A one-tap SOS flow: a breathing pause, 90 seconds of guided urge-surfing,
  and your own if-then plan shown back to you when you need it
- A calm pause at the door of apps you choose, during hours you choose (Premium)
- Gentle progress: waves surfed, trends, and how quickly you come back after a
  slip — never a streak that resets to zero
- Private by design: no account, no cloud, no analytics. The app has no
  internet permission — your data cannot leave your phone

Choose what you're working on during setup — endless feeds, adult content, or
both — and Stillwater adapts how it talks with you.

Free forever: the SOS flow, logging, and one if-then plan.
Premium: app protection, insights, unlimited plans. 7-day free trial.

**Keywords to weave into the listing naturally:** digital wellbeing, screen
time, impulse control, urge, focus, quit scrolling, app blocker, mindful,
habit.
**Never use in the listing:** any explicit/adult terms; "addiction" claims;
medical/neuroscience claims ("dopamine", "rewire your brain").

## 4. Products to configure in Play Console

- Subscription id: `stillwater_premium`
  - Base plan `annual` — ₹599/yr (IN) / $29.99/yr default, with a 7-day free
    trial offer, using Play's per-country price templates
  - Base plan `monthly` — no trial
- License testers (Play Console → Settings → License testing): add your
  closed-test users so they can exercise purchases without being charged.

## 5. Pre-launch checklist

- [ ] Host PRIVACY_POLICY.md publicly; paste URL in Play Console
- [ ] Closed test: 12+ testers, 14 continuous days (start the clock early)
- [ ] Verify on a real device: FGS start from BOOT_COMPLETED on Android 15+
      (documented restriction while holding SYSTEM_ALERT_WINDOW), OEM battery
      wizards on Xiaomi/Samsung, haptics feel, overlay over real social apps
- [ ] Replace the placeholder launcher icon before listing
- [ ] Turn off debug premium preview is automatic (release builds gate on real
      entitlement) — verify with a license tester
- [ ] App signing: enroll in Play App Signing; keep the upload key safe
