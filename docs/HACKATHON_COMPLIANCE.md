# Hackathon Compliance Audit

Audit date: 16 October 2026. The official source checked was [OFFICIAL RULES.md](https://github.com/microsoft/insidethegamehackathon/blob/main/OFFICIAL%20RULES.md). This document reports repository evidence; it does not certify eligibility, authorship, submission, deployment, or prize eligibility.

## Executive status

**Conditionally ready for the submission checklist.** The local application, synthetic-data pipeline, upload path, deterministic AI fallback, replay overlay, and automated checks are implemented. Live Gemini execution has not been verified with a replacement key, and no Microsoft Foundry integration is present. Public repository, demo URL, video, pitch submission, and entrant eligibility remain external tasks.

## Requirement matrix

| Requirement | Status | Repository evidence | Verification | Remaining limitation |
|---|---|---|---|---|
| New project and entrant eligibility | BLOCKED | Not inferable from source | No repository history is available; no entrant records are in scope | Entrant must confirm dates, eligibility, originality, and ownership |
| Synthetic football-realistic data | PASS | `dataset/`, `dataset/README.md`, `generation_manifest.json` | Java loader validated 12 matches and 1,262 events; 22-check report is present | Independent Python generator validator is not included |
| Ingest stage | PASS | `DatasetLoader`, `UploadController`, replay endpoint | Maven tests and startup validation; valid/invalid CSV tests | Replay is simulated, not an external live feed |
| Interpret stage | PASS | `MatchAnalysisService` | Statistics, score reconstruction, turning-point and evidence tests pass | Only metrics supported by the dataset are shown |
| Explain stage | PARTIAL | `AIService` Gemini REST/function calling plus DEMO fallback | Missing-key Gemini fallback verified; live Gemini request not verified | A newly generated Gemini key is required for live-provider verification |
| Render stage | PASS | React dashboard, replay panel, `/replay` DTO | Frontend tests/build pass; runtime API smoke tests were previously performed | Browser click-through was not executed in this audit |
| Personalize stage | PASS | Fan/Analyst modes and `preference` in `AIService`/`AITab` | Recap and deterministic Q&A tests pass | No language-selection mode is implemented |
| Narrative generation and explainability | PARTIAL | Gemini prompt requires tools/evidence; deterministic narratives | DEMO output is deterministic and labelled; Gemini schema/evidence validation is implemented | Model-produced narrative with a valid new key remains unverified |
| Best Use of Microsoft Foundry category | FAIL | No Foundry SDK, endpoint, agent, or credential path | Source audit found Gemini only | Do not claim this category without genuine Foundry usage |
| Multi-agent category | NOT APPLICABLE | Single focused evidence workflow | No multi-agent architecture is claimed | A single workflow is intentional and sufficient for this scope |
| Public GitHub repository | BLOCKED | No Git metadata or public URL in workspace | `git` reports this directory is not a repository | User must create/publish and provide a public URL |
| Demonstration video under two minutes | BLOCKED | No video URL/file | Not created automatically | User must record/publish an allowed public video showing the real app |
| Judge-accessible working demo/test build | BLOCKED | Local Docker/development files exist | Local builds pass; no public deployment verified | User must deploy or provide a test build and access instructions |
| English submission materials | PASS | README and docs are English | Files inspected | Final pitch/video must also satisfy this rule |
| IP, licensing, and third-party integration authorization | PARTIAL | Synthetic data and open-source dependencies are documented | No proprietary footage is bundled | User must verify ownership/licenses and third-party API terms before submission |
| No secrets in source/frontend | PASS | Backend-only `GEMINI_API_KEY`; ignored `.env`; removed `.env.save` | Repository scan found no key literal in tracked application files; no Git history exists to audit | Revoke any key previously pasted into chat or other logs |

## AI provider truth table

| Item | Truthful status |
|---|---|
| Configured provider | Gemini, selected by `AI_MODE=GEMINI`; DEMO is the default |
| Authentication | Backend `GEMINI_API_KEY` sent as `x-goog-api-key`; never sent by React |
| Genuine Gemini request verified | No; replacement credential was not available for this audit |
| Gemini tool calling verified | No live request; code implements five backend-controlled function declarations and bounded execution |
| Evidence grounding verified | DEMO evidence grounding and model-output evidence-ID validation are verified in code/tests; live Gemini grounding is blocked by credentials |
| Fallback verified | Yes; missing-key Gemini mode returns `GEMINI_UNAVAILABLE`, while DEMO returns `DEMO` |
| Microsoft Foundry integrated | No |
| Supported category claim | General challenge / Grand Prize ambition only, subject to Microsoft-platform expectations; not Best Use of Foundry |

## Commands and results

| Command | Directory | Result |
|---|---|---|
| `mvn clean test -q` | `backend/` | PASS — 10 tests, 0 failures, 0 errors |
| `npm test -- --run` | `frontend/` | PASS — 1 test file, 1 test |
| `npm run build` | `frontend/` | PASS — TypeScript/Vite production build |
| `python3 -m tools.synthetic_data.validate dataset` | repository root | BLOCKED — module `tools` is not present in this repository |
| Backend startup dataset validation | `backend/` | PASS — 12 matches, 1,262 events, 10 teams, 180 players |
| Live Gemini recap/Q&A | running backend with replacement key | NOT RUN — no valid replacement key was used |
| Browser E2E workflow | browser | NOT RUN in this audit |

## External submission checklist

The entrant must still confirm or complete:

- register/submit through the hackathon website before the stated deadline;
- provide a public GitHub repository URL;
- provide a public, free, judge-accessible deployment or test build;
- record and publish a video under two minutes on an allowed platform, without unlicensed trademarks, footage, or music;
- provide the pitch describing the actual technologies used (Gemini, Spring Boot, React, synthetic data) and not claim Foundry;
- verify originality, open-source/API licenses, eligibility, team representation, and any required English translations;
- configure and test a newly generated Gemini key without exposing it in source control or the frontend.
