# DiaryMind — Deferred Items

## From CEO Review
- [ ] Gemini Nano pipeline (device coverage limited)
- [ ] Obsidian Vault sync (niche feature — validate demand first)
- [ ] Desktop widget (nice-to-have, not core loop)
- [ ] Calendar view (v1.5+ — timeline sufficient for MVP)
- [ ] PERMA trend charts (requires 30+ days data accumulation)
- [ ] Share Sheet extension (v1.2 — significant Android complexity)
- [ ] User research on PERMA vs. simple mood scoring
- [ ] Competitive analysis: Obsidian plugin as alternative MVP path

## From Design Review
- [ ] Define home screen wireframe and navigation structure
- [ ] Design empty states for all lists and content areas
- [ ] Design error states (AI pipeline failure, API key invalid, offline)
- [ ] Design loading states (optimistic update vs. progress indicator)
- [ ] Provide "fragment → diary" quality benchmark examples
- [ ] Design notification copy and open animation for diary completion
- [ ] Design "review tone" setting UI (gentle/direct/off)
- [ ] Define core interaction gestures (swipe, long-press, tap)
- [ ] Clarify Markdown rendering scope (Obsidian syntax handling)

## From Engineering Review
- [ ] Pipeline state machine with persistent step tracking
- [ ] Fragment-Diary association table (many-to-many)
- [ ] API key storage with EncryptedSharedPreferences + Keystore
- [ ] Privacy consent dialog for external API selection
- [ ] Input validation for share extension (length, control chars)
- [ ] Batch processing queue for large fragment counts
- [ ] WorkManager expedited mode + Doze handling
- [ ] Room migration tests
- [ ] Offline fallback strategy (external API → degraded mode)
- [ ] Cross-midnight fragment date attribution rules
