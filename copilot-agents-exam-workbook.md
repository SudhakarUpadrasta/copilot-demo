# GitHub Actions + Copilot Agents — Exam Workbook

Nine chapters, one for each focus area from the candidate's guidance. Every chapter has the same layout:

1. **Learning goals** – what you must be able to do
2. **Core concepts** – the facts to memorize
3. **Syntax & examples** – annotated, copy-paste-able
4. **Exam traps** – where wrong answers hide
5. **Hands-on lab** – do it, don't just read it
6. **Self-check** – answer without looking

> **Accuracy note.** Copilot CLI and custom-agent features move fast. Chapters 3–8 were checked against the GitHub Docs at the time of writing (Nov 2026). Anything marked ⚠️ *verify* is something I could not fully confirm. Run `copilot --help` and check docs.github.com before the exam.

**Suggested study order:** 1 → 2 → 5 → 6 → 7 → 8 → 3 → 4 → 9. Chapters 5–8 are one connected topic (what an agent file contains, what it can use, how it connects to MCP, how it is governed).

---

# Chapter 1 — GitHub Actions syntax and context values

## 1.1 Learning goals
- Predict the value of `github.ref`, `github.head_ref`, `github.sha`, `run_id`, `run_number` for a given event.
- Write a correct job-to-job data hand-off using `needs` + outputs + `$GITHUB_OUTPUT`.
- Choose between **outputs** and **artifacts**.
- Write least-privilege `permissions`.
- Build static and dynamic matrices.
- Write a `concurrency` block and explain exactly what `cancel-in-progress` does.

## 1.2 Core concepts

### 1.2.1 `actions/checkout`
| Fact | Detail |
|---|---|
| Default checkout | The event's ref/SHA. On `pull_request` it checks out the **merge ref** `refs/pull/<N>/merge` (PR merged into base), not the head branch tip. |
| `ref:` input | Override with a branch, tag or SHA. |
| `fetch-depth` | Default `1` (shallow). Use `0` for full history (needed for `git log`, diffs against base, tags). |
| `persist-credentials` | Default `true`: token stays in `.git/config` so later `git push` works. Set `false` if you don't want later steps to push. |
| `token:` | Defaults to `github.token`. Use a PAT/App token when a push must trigger *other* workflows. |
| Needs `contents: read` | If you set `permissions:` and omit `contents`, checkout of a private repo fails. |

**Checking out the real PR head instead of the merge commit:**
```yaml
- uses: actions/checkout@v4
  with:
    ref: ${{ github.event.pull_request.head.sha }}
```

### 1.2.2 Refs and context values (memorize this table)
| Context | `push` to `main` | `pull_request` #42 (feature → main) |
|---|---|---|
| `github.ref` | `refs/heads/main` | `refs/pull/42/merge` |
| `github.ref_name` | `main` | `42/merge` |
| `github.head_ref` | *(empty)* | `feature` (source branch) |
| `github.base_ref` | *(empty)* | `main` (target branch) |
| `github.sha` | the pushed commit | the **merge commit** SHA, not the PR tip |
| `github.event.pull_request.head.sha` | n/a | the PR tip commit |
| `github.event_name` | `push` | `pull_request` |

- **`head_ref` and `base_ref` are only populated for `pull_request` / `pull_request_target`.** On `push` they are empty strings. This is why concurrency groups use `github.head_ref || github.run_id`.
- `github.ref_type` is `branch` or `tag`.

### 1.2.3 Run identifiers
| Value | Behaviour |
|---|---|
| `github.run_id` | Unique across the **repository**, per workflow run. **Does not change on re-run.** |
| `github.run_number` | Sequential counter **per workflow**, starting at 1. **Does not change on re-run.** |
| `github.run_attempt` | Starts at 1, **increments on each re-run** of the same run. |
| `github.job` | The job id of the current job. |

Typical use: unique artifact names (`results-${{ github.run_id }}-${{ github.run_attempt }}`), unique concurrency groups, build versions.

### 1.2.4 `needs`, job outputs and `$GITHUB_OUTPUT`

Jobs run **in parallel by default**. `needs:` creates ordering **and** unlocks the `needs` context.

```yaml
jobs:
  producer:
    runs-on: ubuntu-latest
    outputs:                                   # (3) map step output -> job output
      sha_short: ${{ steps.vars.outputs.sha_short }}
      changed: ${{ steps.vars.outputs.changed }}
    steps:
      - id: vars                               # (1) step MUST have an id
        run: |
          echo "sha_short=${GITHUB_SHA::7}" >> "$GITHUB_OUTPUT"   # (2) write name=value
          echo "changed=true"               >> "$GITHUB_OUTPUT"

  consumer:
    needs: producer                            # (4) declare dependency
    runs-on: ubuntu-latest
    steps:
      - run: echo "Built ${{ needs.producer.outputs.sha_short }}"  # (5) read via needs
```

Rules:
- Writing is done with `echo "name=value" >> "$GITHUB_OUTPUT"`. The old `::set-output` command is **deprecated/removed**.
- Step output read in the same job: `steps.<step_id>.outputs.<name>`.
- Job output read in a later job: `needs.<job_id>.outputs.<name>`, and only if that job is listed in `needs` (directly; transitively listed jobs are not automatically readable).
- All outputs are **strings**. Booleans/numbers compare as strings: use `== 'true'`.
- Multi-line values need a delimiter:
  ```bash
  {
    echo "report<<EOF"
    cat report.txt
    echo "EOF"
  } >> "$GITHUB_OUTPUT"
  ```
- `needs.<job>.result` is `success`, `failure`, `cancelled` or `skipped`.
- By default a job is **skipped if any needed job failed**. Override with `if: ${{ always() }}` or `if: ${{ !cancelled() }}`.
- Related files: `$GITHUB_ENV` (env vars for later steps, same job), `$GITHUB_STEP_SUMMARY` (markdown on the run summary page), `$GITHUB_PATH`.
- Size limits (⚠️ *verify current numbers*): about 1 MB per job output and about 50 MB total per run. Outputs that contain secrets are redacted/dropped.

### 1.2.5 Artifacts (`actions/upload-artifact` / `download-artifact` v4)
```yaml
# producer job
- uses: actions/upload-artifact@v4
  with:
    name: review-${{ matrix.agent }}        # v4: names must be UNIQUE within a run
    path: out/review.json
    retention-days: 7
    if-no-files-found: error

# consumer job (needs: producer)
- uses: actions/download-artifact@v4
  with:
    pattern: review-*                       # download several...
    merge-multiple: true                    # ...into one folder
    path: reviews/
```
| Outputs | Artifacts |
|---|---|
| Small strings (SHA, flag, JSON list) | Files / directories / logs / reports |
| Passed through `needs` | Passed through upload/download actions |
| Not downloadable from UI | Downloadable from the run summary page, retained per `retention-days` |
| No separate audit event | Deleting one generates an audit-log event (see Chapter 9) |

v4 behaviour: artifacts are **immutable** (you can't append), uploading the same name twice in one run **fails**, and artifacts from a run can only be downloaded within that run unless you pass `run-id` + `github-token`.

### 1.2.6 `permissions`
```yaml
permissions:          # workflow level
  contents: read

jobs:
  comment:
    permissions:      # job level OVERRIDES workflow level for that job
      contents: read
      pull-requests: write
```
- Once you declare **any** scope, every scope you didn't list becomes `none`.
- `permissions: {}` = no permissions.
- Common scopes: `contents`, `pull-requests`, `issues`, `actions`, `checks`, `packages`, `id-token` (OIDC), `security-events`, `statuses`.
- Each is `read`, `write` or `none` (`id-token` has only `write`/`none`).
- PRs from **forks** get a read-only token regardless of what you request, and no secrets (except via `pull_request_target`, which is dangerous with untrusted code).

### 1.2.7 Matrix and list values
```yaml
strategy:
  fail-fast: false            # don't cancel siblings when one fails
  max-parallel: 2
  matrix:
    agent: [security, style, tests]        # 3 jobs
    os: [ubuntu-latest, windows-latest]    # x2 = 6 jobs
    exclude:
      - agent: style
        os: windows-latest
    include:
      - agent: security
        os: ubuntu-latest
        extra: "--strict"                  # adds a variable to matching combos
```
Read with `${{ matrix.agent }}`.

**Dynamic matrix from a previous job (very testable):**
```yaml
jobs:
  plan:
    runs-on: ubuntu-latest
    outputs:
      agents: ${{ steps.p.outputs.agents }}
    steps:
      - id: p
        run: echo 'agents=["security","style","tests"]' >> "$GITHUB_OUTPUT"

  review:
    needs: plan
    strategy:
      matrix:
        agent: ${{ fromJSON(needs.plan.outputs.agents) }}
    runs-on: ubuntu-latest
    steps:
      - run: echo "running ${{ matrix.agent }}"
```
Helpers: `fromJSON()`, `toJSON()`, `join(array, ', ')`, `contains(x, y)`, `format()`, `startsWith()`, `hashFiles()`.
**A job output from a matrix job is the value from whichever leg finishes last/non-empty**, so use artifacts (one per leg) for per-leg results.

### 1.2.8 `concurrency` and `cancel-in-progress`
```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.head_ref || github.run_id }}
  cancel-in-progress: true
```
- Only **one running** and **one pending** run per `group`.
- Without `cancel-in-progress`, a new run **waits** (queued). If an even newer run arrives, the older *pending* run is replaced/cancelled, but the *running* one finishes.
- With `cancel-in-progress: true`, the **in-progress** run is cancelled as soon as a new one in the same group starts.
- `github.head_ref || github.run_id` means: for PRs group per source branch (new pushes cancel old runs); for pushes (empty head_ref) fall back to a unique id so nothing is cancelled.
- Can be set at workflow level or job level.
- `cancel-in-progress` can be an expression: `${{ github.ref != 'refs/heads/main' }}` (cancel on feature branches, never on main).
- Group names are **case-insensitive**.

## 1.3 Exam traps
1. **"`github.head_ref` on a push event"** → empty.
2. **"`github.sha` on a PR is the PR's last commit"** → false, it's the merge commit.
3. **"Re-run changes `run_id`"** → false; `run_attempt` changes.
4. **"Job B can read `needs.A.outputs.x` without listing A"** → no.
5. **Forgetting `id:` on the step** → `steps.x.outputs` is empty.
6. **Using `echo "::set-output..."`** → outdated.
7. **Declaring one permission and expecting the rest to stay default** → they become `none`.
8. **Concurrency without `cancel-in-progress` and thinking runs are cancelled** → they queue.
9. **Two matrix legs uploading the same artifact name (v4)** → failure; add the matrix value to the name.

## 1.4 Hands-on lab
1. Create a repo with a PR trigger. Add a step that prints `github.ref`, `ref_name`, `head_ref`, `base_ref`, `sha`, and `github.event.pull_request.head.sha`. Trigger on a push and on a PR; fill in the table in 1.2.2 from real output.
2. Re-run the workflow from the UI; record `run_id`, `run_number`, `run_attempt` before and after.
3. Build `plan → review (matrix) → summarize`. `plan` emits a JSON list; each `review` leg uploads `review-<agent>`; `summarize` downloads with `pattern: review-*` and writes to `$GITHUB_STEP_SUMMARY`.
4. Add `concurrency` with `cancel-in-progress: true`. Push twice quickly; observe the cancelled run. Switch it to `false`; observe queueing.
5. Remove `pull-requests: write` from a job that comments; see the 403, then fix it.

## 1.5 Self-check
1. What does `github.head_ref` contain for `workflow_dispatch`?
2. Write the exact line that sets an output named `version`.
3. How do you consume a JSON array output as a matrix?
4. A workflow sets only `contents: read`. Can it comment on a PR? Why or why not?
5. What happens to a *running* run when a new run enters the same group with `cancel-in-progress: false`?
6. Output vs artifact: which do you use for a 3 MB report file?

<details><summary>Answers</summary>

1. Empty string. It's only set on pull_request events.
2. `echo "version=1.2.3" >> "$GITHUB_OUTPUT"`
3. `matrix: { item: ${{ fromJSON(needs.job.outputs.name) }} }`
4. No. Commenting needs `pull-requests: write` (or `issues: write`). Listing only `contents` sets all others to `none`.
5. It keeps running; the new run waits as pending.
6. Artifact.
</details>

---

# Chapter 2 — Agent workflow patterns

## 2.1 Learning goals
- Design multi-agent flows where several agents act on the same PR/branch without stepping on each other.
- Implement **reviewer / auditor / consolidator** jobs.
- Implement **producer → artifact/output → consumer** hand-offs.
- Explain how `needs`, `concurrency`, `permissions` and artifacts combine to make these flows safe and auditable.

## 2.2 Core concepts

### 2.2.1 Why patterns matter
An "agent" in this context is any automated actor (Copilot CLI in a workflow, cloud agent, or a custom agent) that reads context and produces changes or findings. Problems appear when:
- Two agents **push to the same branch** → non-fast-forward rejects, lost work.
- One agent needs **another agent's output** → ordering and transport needed.
- Findings must be **reviewable by humans** → persisted evidence needed.

### 2.2.2 Pattern A — Fan-out / fan-in (reviewer → consolidator)
```
          ┌─ review-security ─┐
 plan ───▶├─ review-style ───▶├──▶ consolidate ──▶ comment on PR
          └─ review-tests ───┘
   (matrix: each leg uploads its own artifact)
```
```yaml
jobs:
  review:
    strategy:
      fail-fast: false
      matrix:
        agent: [security, style, tests]
    runs-on: ubuntu-latest
    permissions: { contents: read }              # reviewers are READ-ONLY
    steps:
      - uses: actions/checkout@v4
        with: { ref: "${{ github.event.pull_request.head.sha }}" }
      - run: copilot -p "Review this PR as the ${{ matrix.agent }} reviewer. Write findings to findings.md" \
             --agent ${{ matrix.agent }}-reviewer --no-ask-user --allow-tool='write' --allow-tool='shell(git:*)'
        env: { COPILOT_GITHUB_TOKEN: "${{ secrets.COPILOT_PAT }}" }   # ⚠️ verify env var name
      - uses: actions/upload-artifact@v4
        with:
          name: findings-${{ matrix.agent }}
          path: findings.md

  consolidate:
    needs: review
    if: ${{ !cancelled() }}                       # run even if one reviewer failed
    runs-on: ubuntu-latest
    permissions: { contents: read, pull-requests: write }   # ONLY this job can write to the PR
    steps:
      - uses: actions/download-artifact@v4
        with: { pattern: findings-*, merge-multiple: true, path: findings }
      - run: cat findings/*.md > comment.md
      - run: gh pr comment ${{ github.event.pull_request.number }} --body-file comment.md
        env: { GH_TOKEN: "${{ github.token }}" }
```
Design principle: **separate read-only analysis jobs from one write-capable job** (least privilege, single point of side effect).

### 2.2.3 Pattern B — Sequential agents on the same branch
Use `needs` to order them so they never push in parallel:
```
implement (agent 1 pushes) ─▶ test-fix (agent 2 pushes) ─▶ audit (agent 3 read-only)
```
Each later job must `checkout` **after** the earlier push (fresh `ref: ${{ github.head_ref }}`), otherwise it works on a stale SHA.

### 2.2.4 Pattern C — Guarding against races with `concurrency`
```yaml
concurrency:
  group: agents-pr-${{ github.event.pull_request.number }}
  cancel-in-progress: false      # don't kill an agent mid-push; queue the next one
```
Choose `true` for *read-only* analysis (stale results are worthless), `false` for anything that **writes** (killing mid-commit can leave a half-done branch).

### 2.2.5 Pattern D — Auditor
An auditor job runs *after* the actors, is read-only, and verifies: the diff touches only allowed paths, no secrets were added, tests pass, workflow files unchanged. It publishes a verdict as an **output** (`pass`/`fail`) that a gate job checks:
```yaml
audit:
  needs: implement
  outputs:
    verdict: ${{ steps.v.outputs.verdict }}
gate:
  needs: audit
  if: needs.audit.outputs.verdict == 'pass'
```

### 2.2.6 Choosing the transport
| Need | Use |
|---|---|
| Boolean/short string, branching | Job `outputs` |
| Per-leg results of a matrix | One uniquely named artifact per leg |
| Large report, logs, patches | Artifact |
| Human-visible summary | `$GITHUB_STEP_SUMMARY` or a PR comment |
| Cross-workflow hand-off | Artifact + `workflow_run` trigger (or `run-id` input to download) |

### 2.2.7 Multiple Copilot agents on one PR (cloud agent)
- Cloud agent works on its own `copilot/…` branch and opens a **draft PR**; follow-ups come via `@copilot` comments on that PR (Chapter 4).
- If you run Copilot CLI/other agents in Actions *against that same branch*, you need serialization (`concurrency`) or you will collide.

## 2.3 Exam traps
- Parallel jobs that both push to the same branch → race. Fix: `needs` ordering or `concurrency`.
- Consolidator that doesn't use `if: ${{ !cancelled() }}`/`always()` is **skipped** when one reviewer fails.
- Matrix legs using one shared artifact name (v4) → upload failure.
- Giving every reviewer `pull-requests: write` → violates least privilege.
- Reading `needs.review.outputs.x` from a matrix when each leg set a different value → unreliable; use artifacts.

## 2.4 Hands-on lab
Build the Pattern A workflow end-to-end with plain shell "agents" (`echo` a fake finding) first, then swap in `copilot -p`. Add: (a) one reviewer that deliberately fails, confirm the consolidator still runs; (b) a `concurrency` group; (c) an auditor that outputs `verdict`.

## 2.5 Self-check
1. Why make reviewers read-only and the consolidator the only writer?
2. Which `if:` lets a consolidator run when a matrix leg failed?
3. When would you pick `cancel-in-progress: false` for an agent job?
4. How does an auditor job communicate pass/fail to a later job?
5. How do you collect results from N matrix legs?

<details><summary>Answers</summary>

1. Least privilege and one controlled side-effect point; easy to audit.
2. `if: ${{ !cancelled() }}` or `always()`.
3. When the job writes (commits/pushes); cancelling mid-write is unsafe.
4. A job output consumed through `needs.<audit>.outputs.verdict`.
5. Each leg uploads a uniquely named artifact; consumer uses `pattern` + `merge-multiple`.
</details>

---

# Chapter 3 — Copilot CLI basics and logs

## 3.1 Learning goals
- Run Copilot CLI interactively and non-interactively.
- Know what each flag/slash command does and which combinations conflict.
- Distinguish **new** vs **resumed** sessions, and find session IDs/logs.
- Control MCP servers from flags.
- Explain how the CLI relates to VS Code.

## 3.2 Core concepts

### 3.2.1 Modes
| Command | Meaning |
|---|---|
| `copilot` | Interactive UI. |
| `copilot -p "prompt"` | **Programmatic / non-interactive**: runs the prompt and exits. Used in scripts and Actions. |
| `copilot --agent <name>` | Start with a specific **custom agent** (matched by agent ID or name). |
| `/agent <name>` | Switch to a custom agent **inside** an interactive session. |
| `--autopilot` | **Autopilot**: Copilot keeps taking steps until it decides the task is done, without waiting for you after each step. Works best with all permissions granted (`--allow-all` / `--yolo`); with limited permissions, tool requests needing approval are auto-denied. |
| `--no-ask-user` | Removes the `ask_user` tool: the agent can't ask clarifying questions and must decide itself. **It does not by itself make the agent continue autonomously across steps** — that's autopilot. |
| `--allow-all` / `--yolo` / `/allow-all` | Grants all permissions (⚠️ includes deleting files). |
| `--allow-tool='shell(git:*)'` / `--deny-tool='shell(git push)'` | Fine-grained permissions. Kinds: `shell`, `write`, `read`, `url`, `memory`, or an MCP server name. |

**Autopilot vs `--no-ask-user` vs `--allow-all`** (classic exam comparison):
| Option | Controls |
|---|---|
| `--autopilot` | **Continuation**: keeps working through multiple steps |
| `--no-ask-user` | **Questions**: agent can't ask you anything |
| `--allow-all` | **Permissions**: no approval prompts |
Full CI autonomy typically = all three (or the `autopilot` + `no-ask-user` defaults of the official CLI GitHub Action).

### 3.2.2 Sessions
| Flag | Behaviour |
|---|---|
| *(none)* | **New session** with a new session ID. |
| `--continue` | Resume the **most recent** session in the current directory (falls back to globally most recent). |
| `--resume` | Open a **session picker** (or `--resume <session-id>` to jump to one). |
| `--connect[=ID]` | Connect to a **remote** session (by session or task ID). **Conflicts with `--resume` and `--continue`.** |
| `--continue` + `--resume` | **Conflict** — can't combine. |
| `/resume` | Same as `--resume`, from inside the CLI. |

- Each session has an **ID**; history is stored locally under the Copilot home (`~/.copilot`, overridable with `COPILOT_HOME`) — ⚠️ *verify exact subfolder name*.
- A **resumed** session keeps its previous conversation context; a **new** session starts blank. In CI, `-p` creates a fresh session each run unless you pass a resume flag (the official Action exposes `resume-session: latest`).
- `/share` exports a session to markdown; `/share gist` creates a Gist.

### 3.2.3 Delegation and parallelism
| Command | Meaning |
|---|---|
| `/delegate [PROMPT]` | Push the work to **Copilot cloud agent**: it commits your changes to a new branch, opens a **draft PR**, and the cloud agent continues there. The session is then monitored via the PR / "View session" (Chapter 4). |
| `/fleet [PROMPT]` | Enable **parallel subagent execution** inside the CLI session: it splits a task into independent parts and runs subagents at the same time. Local, in-session parallelism — not the same as `/delegate`. |

Quick contrast: **`/delegate` = hand off to the cloud (async, PR).** **`/fleet` = parallelize locally (sync, subagents).**

### 3.2.4 MCP flags and commands
| Flag / command | Purpose |
|---|---|
| `--additional-mcp-config <json or @file>` | Add MCP servers for this session only. |
| `--disable-mcp-server <name>` | Disable one server (repeatable). |
| `--disable-builtin-mcps` | Disable all built-ins (currently the GitHub MCP server). |
| `--add-github-mcp-toolset <set>` / `--add-github-mcp-tool <tool>` / `--enable-all-github-mcp-tools` | Widen the built-in GitHub MCP server. |
| `/mcp show\|add\|edit\|delete\|disable\|enable\|auth\|reload` | Manage servers in-session. |
| `copilot mcp add --transport stdio\|http\|sse NAME ...` | Add from the shell; default transport is `stdio`; for local: `copilot mcp add NAME -- COMMAND ARGS...` |
| `copilot plugins disable github --mcp` | Persistently disable an MCP server for future sessions. |

User-level MCP config lives in `~/.copilot/mcp-config.json` (JSON key `mcpServers`).

### 3.2.5 Editor / VS Code attachment
The CLI can connect to an open editor so it sees workspace context (open files, selections, diffs) and shows changes in the editor (`/ide` ⚠️ *verify command name in your CLI version*). Sessions can also be surfaced in VS Code's agent/session views. Remember: **CLI = terminal agent; VS Code = editor with its own agent mode; the two can share context but are separate harnesses.**

### 3.2.6 Logs
- In the CLI: `--log-level` / `--log-dir` (⚠️ *verify*) and session history in the Copilot home.
- In **Actions** (running the CLI): the step log of the `copilot -p` step is your primary log; add `$GITHUB_STEP_SUMMARY` and upload CLI log directory as an **artifact** for evidence.
- `--log-level error|warning` reduces noise (per the official Action docs).

## 3.3 Exam traps
- `-p` is **prompt (non-interactive)**, not "path/profile".
- `--no-ask-user` ≠ autopilot.
- `--continue` vs `--resume`: continue = most recent automatically; resume = pick (or ID).
- `--connect` can't be mixed with `--resume`/`--continue`.
- `/delegate` ≠ `/fleet`.
- `--disable-mcp-server` (one) vs `--disable-builtin-mcps` (all built-in).
- Default transport for `copilot mcp add` is `stdio`.

## 3.4 Hands-on lab
1. `copilot -p "List the files in this repo" --allow-tool='shell(ls:*)'` — note it exits.
2. Start an interactive session, ask something, exit; run `copilot --continue`; confirm context survived. Run `copilot --resume` and open the picker.
3. Run the same prompt with and without `--no-ask-user` using an ambiguous request; compare behaviour.
4. Run with `--disable-builtin-mcps`, then ask it to list issues; observe failure.
5. `/delegate` a trivial change; open the PR it creates.

## 3.5 Self-check
1. Which flag runs a one-shot prompt and exits?
2. Which flag stops the agent asking you questions? Does it make it keep going autonomously?
3. Difference between `/delegate` and `/fleet`?
4. Which flags can't be combined with `--connect`?
5. How do you disable only the `playwright` MCP server for a run?
6. Does `copilot -p` reuse the previous session by default?

<details><summary>Answers</summary>

1. `-p`/`--prompt`.
2. `--no-ask-user`; no — autopilot handles continuation.
3. `/delegate` sends to cloud agent/draft PR; `/fleet` runs parallel subagents locally.
4. `--resume`, `--continue`.
5. `--disable-mcp-server=playwright`.
6. No, a new session unless `--continue`/`--resume`.
</details>

---

# Chapter 4 — Copilot cloud agent lifecycle

## 4.1 Learning goals
- Describe the full life of a cloud-agent task from assignment to merge.
- Know where to watch it (PR timeline, **View session**, Agents page).
- Fix stuck work (unassign/reassign, re-comment, `@copilot`).
- Explain the **approval gate** for workflows on Copilot PRs.

## 4.2 Core concepts

### 4.2.1 Timeline of a task
```
Assign issue to Copilot  (or /delegate, or ask from Chat / Agents page)
   │  👀 reaction on the issue
   ▼
Copilot creates branch  copilot/<something>   +  opens a DRAFT PR linked to the issue
   │  PR timeline: "Copilot started work"
   ▼
Agent session runs in an Actions-powered, firewalled environment
   │  Live log: PR → "View session"   (Stop session button available)
   ▼
"Copilot finished work"  → session ends → Copilot requests YOUR review
   ▼
You review; iterate with  @copilot  comments;  approve + merge
```
Key facts:
- PRs from Copilot are **always draft** and require **human approval**. The person who asked Copilot to create the PR **cannot approve it** (preserves required-review rules).
- Only comments from users with **write** access are processed.
- `@copilot` mentions work only on **open** PRs; after merge/close Copilot won't respond.
- Later comments on the **issue** are not seen by the agent once it started; put new info on the **PR**.
- Copilot acknowledges a PR comment with 👀, then adds "Copilot started work" to the timeline.
- Sessions time out after **one hour** if stuck.

### 4.2.2 Where to see what happened
| Source | What it gives you |
|---|---|
| **PR timeline** | "Copilot started work" / "Copilot finished work" events, assignment changes, commits |
| **View session** (button on the PR / timeline event) | Live-streaming then archived **session log**: reasoning, tool calls, commands, blocked firewall requests |
| **Agents page** | All sessions past and present |
| **PR body** | Status updates Copilot keeps editing; firewall-block warnings appear here (issue-assigned) or in a comment (comment-triggered) |

### 4.2.3 Stuck or silent — decision table
| Symptom | Fix |
|---|---|
| Copilot opened a PR but "nothing is happening" | Check "Copilot started work" in the timeline; open **View session**. |
| No reaction to your PR comment | Confirm Copilot is still **assigned** to the PR, you have **write** access, the PR is **open**; then force it with **`@copilot`** in the comment. |
| Session looks stuck (logs stalled) | Wait (it may recover); it **times out after 1 hour**. Then **unassign Copilot and reassign** it. If it stalled while answering a comment, **post the same comment again**. |
| Need to stop it now | **Stop session** in the session viewer. |
| Copilot not in the Assignees list | Plan/seat not entitled; admin disabled cloud agent for repo/org; **EMU personal repos are unsupported** (needs GitHub-hosted runners). |

### 4.2.4 The workflow approval gate
- **Actions workflows do not run automatically when Copilot pushes** to its PR. A maintainer must click **Approve and run workflows** in the PR merge box.
- Purpose: Copilot-authored code (including any edits to `.github/workflows/*`) must not execute with repository secrets/permissions before a human has looked at it.
- The approver is a human with the proper rights; this is separate from the **PR review approval**.
- Related: Copilot can be blocked by the **firewall** (outbound allowlist); the block is reported in the PR body/comment with the blocked address and command. Fix via repo settings → Copilot → cloud agent → firewall allowlist, or `COPILOT_AGENT_FIREWALL` customization (⚠️ *verify the setting name*).

## 4.3 Exam traps
- "Copilot's PR is ready for merge" → it's **draft**, needs review and the workflow approval click.
- "The issue author who triggered Copilot can approve its PR" → no.
- "CI runs automatically on Copilot pushes" → no, click **Approve and run workflows**.
- "Add more instructions by editing the issue" → comment on the **PR** with `@copilot`.
- "Session never stops" → 1-hour timeout.

## 4.4 Hands-on lab
1. Create an issue ("add a README badge"), assign to Copilot; note branch name, 👀, draft PR, "Copilot started work".
2. Click **View session**; find a shell command and a file edit in the log.
3. Comment `@copilot please also add a LICENSE badge`; watch a second "started work".
4. Observe that CI is waiting; click **Approve and run workflows**.
5. Unassign and reassign Copilot; observe a new session.

## 4.5 Self-check
1. What's the exact button name that releases Actions on a Copilot PR?
2. How do you reach the live logs from a PR?
3. Two actions to recover a session that is stuck for an hour?
4. Who can't approve Copilot's PR?
5. Why does `@copilot` fail on a merged PR?

<details><summary>Answers</summary>

1. **Approve and run workflows**.
2. Click **View session** on the "Copilot started work" timeline event / PR.
3. Unassign then reassign Copilot (or repeat the comment if it was a comment-driven run).
4. The user who asked Copilot to create it.
5. Copilot only responds on open PRs.
</details>

---

# Chapter 5 — Custom agent files and syntax

## 5.1 Learning goals
- Place every customization file in the correct folder.
- Write a valid `.agent.md` with correct frontmatter.
- Know which field applies to which surface (cloud agent, CLI, VS Code).

## 5.2 Core concepts

### 5.2.1 Where things live
| Path | Purpose |
|---|---|
| `.github/agents/NAME.agent.md` (also `.md`) | **Custom agent profile** (frontmatter + prompt). |
| `.github/copilot-instructions.md` | Repo-wide custom instructions. |
| `.github/instructions/*.instructions.md` | Path-scoped instructions; frontmatter `applyTo: "**/*.ts"`. |
| `.github/prompts/*.prompt.md` | **Prompt files** (reusable slash-command-like prompts, mainly VS Code). |
| `.github/skills/<skill-name>/SKILL.md` | **Agent skills**: folder with `SKILL.md` (frontmatter `name`, `description`) plus scripts/resources, loaded on demand when relevant. |
| `.github/hooks/*.json` | **Hooks** (Chapter 8). |
| `AGENTS.md` | Cross-agent instructions file (recognized by Copilot). |
| `.github-private/agents/` (org/enterprise) | Org-/enterprise-wide agents. **Lowest level wins deduplication**: the repo-level file with the same name (minus `.md`/`.agent.md`) overrides org/enterprise. |

Mnemonic: **agents** = *who*, **instructions** = *always-on rules*, **prompts** = *reusable asks*, **skills** = *loadable know-how*, **hooks** = *guardrails/automation*.

### 5.2.2 Frontmatter fields
```yaml
---
name: security-reviewer            # optional; defaults to filename minus .md/.agent.md
description: Reviews diffs for security issues   # REQUIRED
tools: ["read", "search", "github/*", "my-mcp/lookup"]   # optional; omit = all tools
model: gpt-5                       # IDE/CLI model choice (⚠️ cloud agent: verify support)
target: github-copilot             # vscode | github-copilot | (unset = both)
user-invocable: true               # default true
disable-model-invocation: false    # default false
mcp-servers:                       # cloud agent / CLI; ignored in VS Code agents
  my-mcp:
    type: local
    command: npx
    args: ["-y", "my-mcp-server"]
    tools: ["*"]
    env:
      API_KEY: ${{ secrets.COPILOT_MCP_API_KEY }}
---
You are a security reviewer. Only report findings, never edit files.
```
| Field | Meaning / default |
|---|---|
| `description` | Required. Used for discovery and for subagent selection. |
| `name` | Defaults to filename. |
| `tools` | List of tool names/aliases (Chapter 6). **Omit** → all tools; `[]` → none; `"*"` → all. |
| `target` | `vscode` or `github-copilot`; **not** the harness selector. Unset = usable in both. |
| `model` | Which model to use; if omitted the currently selected model is used (IDE). |
| `user-invocable` | Controls whether a **user** can pick the agent in the dropdown. `false` = hidden, subagent/programmatic only. Default `true`. |
| `disable-model-invocation` | When `true`, Copilot will **not** auto-select this agent as a subagent by itself. Default `false`. |
| `infer` | **Retired/deprecated** — replaced by the two fields above. |
| `mcp-servers` | Extra MCP servers for the agent. **Not used in VS Code / IDE agents.** |
| `handoffs`, `argument-hint` | VS Code/IDE features; **ignored by the cloud agent**. |
| prompt body | The agent's instructions (Markdown below the frontmatter; size-limited, ~30,000 chars ⚠️ *verify*). |

**Hide a helper from the picker but still let other agents call it:**
```yaml
user-invocable: false
disable-model-invocation: false
```
**Make it manual-only (never auto-chosen):** `disable-model-invocation: true`.

### 5.2.3 Instructions file example
```markdown
---
applyTo: "**/*.py"
---
Use type hints. Prefer pathlib over os.path.
```

### 5.2.4 Skill example (`.github/skills/changelog/SKILL.md`)
```markdown
---
name: changelog
description: How to write release notes in our format. Use when asked to draft a changelog.
---
1. Group entries by Added/Changed/Fixed...
```

## 5.3 Exam traps
- `.github/agents/x.agent.md` — not `.github/agents/x.yml`.
- `mcp-servers` in an agent file is **YAML**, hyphenated (Chapter 7).
- `user-invocable: false` doesn't disable the agent entirely; it removes it from the **user** picker.
- `infer` is deprecated.
- `target: github-copilot` refers to the **cloud agent environment**, not "Copilot harness in VS Code".
- `description` is mandatory; `name` is not.
- Prompts (`.prompt.md`) are **not** agents.

## 5.4 Hands-on lab
Create `.github/agents/auditor.agent.md` with `tools: ["read","search"]`, plus a `.github/instructions/tests.instructions.md` with `applyTo`, a `.github/skills/foo/SKILL.md`, and a `.github/prompts/summarize.prompt.md`. Call the agent via `copilot --agent auditor -p "..."`, then via `/agent auditor`. Flip `user-invocable: false` and see it vanish from the picker.

## 5.5 Self-check
1. Which folder holds custom agents? Which extension?
2. Which field hides an agent from the dropdown?
3. What replaced `infer`?
4. If `tools` is omitted, which tools does the agent get?
5. Where do skills live and what must the folder contain?
6. Repo and org both define `reviewer.agent.md`. Which wins?

<details><summary>Answers</summary>

1. `.github/agents/`, `.agent.md`.
2. `user-invocable: false`.
3. `user-invocable` and `disable-model-invocation`.
4. All available tools.
5. `.github/skills/<name>/SKILL.md`.
6. The repository-level one (lowest level takes precedence).
</details>

---

# Chapter 6 — Agent tools

## 6.1 Learning goals
- Map each tool alias to what it really permits.
- Write minimal tool lists for read-only, editing and autonomous agents.

## 6.2 Core concepts

### 6.2.1 Tool aliases (case-insensitive; names from other tools are accepted as aliases)
| Alias | Also matches | What it actually allows |
|---|---|---|
| `execute` | `shell`, `Bash`, `powershell` | Run **shell commands** (any command your sandbox permits). Highest risk. |
| `read` | `Read`, `NotebookRead` | **Read file contents** in the repo/workspace. |
| `edit` | `Edit`, `MultiEdit`, `Write`, `NotebookEdit` | **Modify/create files**. |
| `search` | `Grep`, `Glob` | **Search the repository / codebase** (find files, grep code). It is **not** internet search. |
| `agent` | `custom-agent`, `Task` | **Invoke another custom agent** (subagent). |
| `web` | `WebSearch`, `WebFetch` | **Fetch web content** (URLs). (Think "web fetch".) Subject to the firewall in the cloud agent. |
| `todo` | `TodoWrite` | Create/maintain a **task checklist** (planning/progress tracking), not a file edit. (Availability varies by surface — ⚠️ *verify*.) |

Take-aways:
- `search` = **repo/code search**; `web` = **web fetch**. Never confuse them.
- `read` does **not** let you write; `edit` does not let you run commands; `execute` is separate from both.
- An agent with `["read","search"]` can analyze but cannot change anything: ideal reviewer/auditor.
- An agent without `agent` cannot delegate to other agents.
- `todo` doesn't touch the repo.

### 6.2.2 Listing rules
```yaml
tools: ["*"]                         # all tools
tools: []                            # no tools at all
# (omit field)                       # all tools
tools: ["read","search","edit"]      # specific aliases
tools: ["github/get_issue"]          # one tool from the github MCP server
tools: ["github/*"]                  # every tool of the github MCP server
tools: ["read","my-mcp/lookup"]      # mix built-ins and MCP
```
Format of MCP tool references: **`server-name/tool-name`**; wildcard **`server-name/*`**.

### 6.2.3 Built-in MCP servers in the cloud agent
- `github`: read-only tools by default, token scoped to the source repository (`github/*` or `github/<tool>`).
- `playwright`: browser automation (localhost only by default).

### 6.2.4 Permission patterns (CLI side)
`--allow-tool='shell(git:*)'` allows only `git` subcommands; `--deny-tool` overrides allow. Tool "kinds": `shell`, `write`, `read`, `url`, `memory`, or an MCP server name. These are *CLI permission* kinds; the *agent file* uses the aliases in 6.2.1.

## 6.3 Exam traps
- "`search` can search the web" → false.
- "`web` searches the repo" → false.
- "`edit` allows running tests" → false; that's `execute`.
- "Empty `tools: []` means all tools" → false, it means none; **omitted** means all.
- "`github/*` equals `*`" → no: `github/*` is only the github MCP server's tools.

## 6.4 Hands-on lab
Write three agents: `reader` (`read`,`search`), `fixer` (`read`,`search`,`edit`,`execute`), `researcher` (`read`,`web`). Ask each to (a) grep for a string, (b) change a file, (c) run `npm test`, (d) fetch a URL. Fill a 3×4 matrix of what worked.

## 6.5 Self-check
1. Which alias lets an agent run `pytest`?
2. Which lets it grep the codebase?
3. Which lets it download a web page?
4. What does `tools: []` do? What does omitting `tools` do?
5. How do you allow every tool of an MCP server named `jira`, but only `get_issue` of `github`?

<details><summary>Answers</summary>

1. `execute`.
2. `search`.
3. `web`.
4. None; all.
5. `tools: ["jira/*", "github/get_issue"]`.
</details>

---

# Chapter 7 — MCP configuration

## 7.1 Learning goals
- Write MCP config in **both** shapes (YAML in agent files, JSON in repo/CLI).
- Pick the right transport and fields.
- Distinguish MCP **tool allowlists** from **network firewall** allowlists.

## 7.2 Core concepts

### 7.2.1 Two syntaxes, same idea
| Where | Key | Format |
|---|---|---|
| Custom agent file frontmatter | **`mcp-servers`** (hyphen) | YAML |
| Repo settings (Copilot → cloud agent → MCP configuration), `~/.copilot/mcp-config.json`, `--additional-mcp-config` | **`mcpServers`** (camelCase) | JSON |
The agent YAML is a **YAML representation of the JSON format** (same sub-properties).

### 7.2.2 Local (process) server
```yaml
mcp-servers:
  docs-lookup:
    type: local               # or stdio (mapped to local in the cloud agent)
    command: npx
    args: ["-y", "@acme/docs-mcp"]
    tools: ["search_docs"]    # or ["*"]
    env:
      API_KEY: ${{ secrets.COPILOT_MCP_API_KEY }}
```
```json
{
  "mcpServers": {
    "docs-lookup": {
      "type": "local",
      "command": "npx",
      "args": ["-y", "@acme/docs-mcp"],
      "tools": ["*"],
      "env": { "API_KEY": "COPILOT_MCP_API_KEY" }
    }
  }
}
```
Rule of thumb: **`command` + `args` ⇒ local/`stdio`** (a process started on the runner/your machine).

### 7.2.3 Remote server
```json
{
  "mcpServers": {
    "ticketing": {
      "type": "http",
      "url": "https://mcp.example.com/mcp",
      "headers": { "Authorization": "Bearer $COPILOT_MCP_TOKEN" },
      "tools": ["*"]
    }
  }
}
```
Rule of thumb: **top-level `url` ⇒ remote transport**: `http` (streamable HTTP, preferred) or `sse` (Server-Sent Events, legacy). The repo/cloud-agent docs list `local`, `stdio`, `http`, `sse`; for compatibility `stdio` is mapped to `local`.

| `type` | Use when |
|---|---|
| `local` / `stdio` | Server is a process: `command` + `args` (+ `env`) |
| `http` | Remote, `url` (+ `headers`) |
| `sse` | Remote, older SSE endpoints, `url` |

### 7.2.4 `tools` inside an MCP definition vs `tools` in the agent
- Inside the server block: `tools: ["*"]` or a list = which of **that server's** tools are exposed.
- In the agent's `tools:`: use **`server/tool`** for one tool or **`server/*`** for all of that server's tools.
- The JSON form needs `tools` to be specified (use `["*"]` for all).

### 7.2.5 Secrets and variables
- Cloud agent secrets for MCP live in the repo's **`copilot` environment**; names must start with **`COPILOT_MCP_`**.
- Agent YAML supports `${{ secrets.COPILOT_MCP_X }}` and `${{ var.COPILOT_MCP_X }}` in `env`/headers; the repo JSON form refers to the variable name.

### 7.2.6 Two different "allowlists" — don't mix them up
| | **MCP allowlist / registry policy** | **Cloud-agent firewall allowlist** |
|---|---|---|
| Question it answers | *Which MCP servers/tools may the agent use?* | *Which network hosts can the agent's sandbox reach?* |
| Where | Agent `tools:` lists, org/enterprise MCP registry policy | Repo/org settings → Copilot → cloud agent → firewall |
| Effect of blocking | Tool not available | Request fails; warning in PR body/comment |
| Remote MCP server? | Must be permitted as a tool **and** its host must be reachable | Host must be allow-listed for the agent to call it |
A local MCP server that downloads packages (`npx`) also needs those registries (npm) reachable in the firewall.

### 7.2.7 CLI handling
Manage with `copilot mcp add --transport http NAME URL`, `copilot mcp add NAME -- cmd args`, `/mcp enable|disable NAME`, `--disable-mcp-server`, `--disable-builtin-mcps`, `--additional-mcp-config`.

## 7.3 Exam traps
- `mcp-servers` (YAML, agent) vs `mcpServers` (JSON). Swapping them = wrong.
- `mcp-servers` is **ignored in VS Code/IDE agents**; it applies to cloud agent and CLI.
- `command`/`args` with `type: http` → inconsistent; `url` with `type: local` → inconsistent.
- `server/*` (all tools of one server) vs `*` (all tools overall).
- Firewall allowlist ≠ MCP allowlist.
- Secret naming: missing `COPILOT_MCP_` prefix → not injected.

## 7.4 Hands-on lab
1. Add a local MCP server via agent YAML (`type: local`, `command`, `args`) and call one of its tools.
2. Add the same server via repo JSON `mcpServers`.
3. Point a remote `http` server at a host *not* in the firewall allowlist; observe the failure; add the host; retry.
4. Limit tools to `server/one_tool` and confirm the others aren't callable.

## 7.5 Self-check
1. Which key holds MCP config in an agent file? In repo JSON?
2. Config has `command` and `args`. Which transport?
3. Config has `url`. Which transports are possible?
4. Which control restricts network reachability?
5. How do you expose all tools of `jira` to the agent?
6. Secret prefix for cloud-agent MCP secrets?

<details><summary>Answers</summary>

1. `mcp-servers`; `mcpServers`.
2. local (`stdio`).
3. `http` or `sse`.
4. The cloud-agent firewall allowlist.
5. `jira/*`.
6. `COPILOT_MCP_`.
</details>

---

# Chapter 8 — Hooks

## 8.1 Learning goals
- Read and write a hooks JSON file.
- Know which event fires when, and which can block/modify.
- Contrast **JSON/CLI hooks** with **SDK hooks**, and `preToolUse` with `permissionRequest`.

## 8.2 Core concepts

### 8.2.1 What hooks are
**External commands that run at lifecycle points** of an agent session — for guardrails, logging, notifications, policy. Supported on **Copilot CLI** and **Copilot cloud agent** (the event set differs; cloud agent supports a subset).

### 8.2.2 Where config lives
| Location | Scope |
|---|---|
| `.github/hooks/*.json` | Repository; **the only hook config present in a cloud-agent job by default** (loaded from the cloned repo) |
| `~/.copilot/hooks/*.json` (or `$COPILOT_HOME/hooks/`) | User (CLI) |
| `.github/copilot/settings.json` / `settings.local.json` (`hooks` field) | Repo inline |
| `~/.copilot/settings.json` (`hooks` field) | User inline |
| Plugin `hooks.json` | Installed plugins |
| `/etc/github-copilot/policy.d/*.json` (Linux/macOS), Windows registry/ProgramData | **Policy hooks — CLI only**, not supported in cloud agent |

### 8.2.3 Format (version 1)
```json
{
  "version": 1,
  "hooks": {
    "preToolUse": [
      {
        "type": "command",
        "bash": "./scripts/check-tool.sh",
        "powershell": "./scripts/check-tool.ps1",
        "cwd": ".github/hooks",
        "env": { "MODE": "strict" },
        "timeoutSec": 10
      }
    ],
    "postToolUse": [
      { "type": "command", "bash": "./scripts/log.sh" }
    ]
  }
}
```
Types: **command** (all events), **http** (cloud agent; URL must be `https://` for `preToolUse`/`permissionRequest`; host must pass the firewall), **prompt**.

### 8.2.4 Events (camelCase names in JSON)
`sessionStart`, `sessionEnd`, `userPromptSubmitted`, **`preToolUse`**, **`postToolUse`**, `postToolUseFailure`, `agentStop`, `subagentStop`, `errorOccurred`, `preCompact`, and CLI-only **`permissionRequest`**. (⚠️ *verify full list in the hooks reference.*)

| Event | When | Can it influence behaviour? |
|---|---|---|
| `preToolUse` | **Before** a tool runs | **Yes** — write JSON to stdout to allow/deny (block) the call; `reason` is fed back to the agent. Fires on **CLI and cloud agent**. |
| `postToolUse` | After a tool **succeeds** | Observe/log; **only SDK programmatic hooks can replace the result**. |
| `postToolUseFailure` | After failure | Can add recovery guidance. |
| `permissionRequest` | Fires **before the permission service** runs (before rules, session approvals, auto allow/deny, user prompt) | **Yes** — `behavior: "allow"` / `"deny"` **short-circuits** the normal permission flow; supports a `matcher` regex on `toolName`. **CLI only.** |
| `subagentStop` | A subagent finishes | `decision: "block"` forces another turn with `reason` |

### 8.2.5 SDK hooks
When embedding Copilot via the SDK you register **callbacks** instead of JSON files: `onPreToolUse`, `onPostToolUse`, `onUserPromptSubmitted`, `onSessionStart`, `onSessionEnd`, `onErrorOccurred`. Same lifecycle, but programmatic — and `onPostToolUse` can **modify/replace the tool result**, which JSON command hooks cannot.

### 8.2.6 `preToolUse` vs `permissionRequest` (high-value contrast)
| | `preToolUse` | `permissionRequest` |
|---|---|---|
| Question | "Should this tool call be run at all?" | "Should the user/permission system be asked, or answered automatically?" |
| Timing | Before tool execution | Before permission evaluation (even earlier in the pipeline) |
| Surfaces | CLI **and** cloud agent | **CLI only** |
| Typical use | Block `rm -rf`, enforce path rules, log commands | Auto-approve safe reads / auto-deny network tools, replacing prompts |

### 8.2.7 Cloud agent specifics
Environment variables in the sandbox: `GITHUB_COPILOT_API_TOKEN`, `GITHUB_COPILOT_GIT_TOKEN`, `COPILOT_AGENT_PROMPT`. User-level hooks, `settings.json`, `config.json` and plugins **don't exist** in the sandbox. Only `bash`/`command` entries are honored for the subset of events that fire there.

## 8.3 Exam traps
- "Hooks are YAML in the agent file" → they're JSON under `.github/hooks/` (v1 schema).
- `preToolUse` JSON vs SDK `onPreToolUse` — same idea, different mechanism.
- "`permissionRequest` works in the cloud agent" → CLI only.
- "A JSON `postToolUse` hook can rewrite the result" → only SDK hooks.
- `http` hooks for `preToolUse` must be **HTTPS**.
- Policy hooks (`/etc/github-copilot/policy.d`) are CLI-only.

## 8.4 Hands-on lab
Write `.github/hooks/guard.json` with a `preToolUse` command hook that reads the JSON payload on stdin and blocks any `tool_name` of `bash` whose input contains `rm -rf`. Test in CLI. Add a `postToolUse` hook that appends each tool call to `audit.log`. Then run the same repo with a cloud-agent task and check which hooks fired.

## 8.5 Self-check
1. File path of repo-level hooks?
2. Which hook can pre-empt the permission prompt, and where is it supported?
3. Which hook type can replace a tool's result?
4. What scheme must an `http` `preToolUse` hook use?
5. Name two SDK hook callbacks.

<details><summary>Answers</summary>

1. `.github/hooks/*.json`.
2. `permissionRequest`; CLI only.
3. SDK programmatic `onPostToolUse`.
4. `https://`.
5. e.g. `onPreToolUse`, `onPostToolUse`.
</details>

---

# Chapter 9 — Auditability

## 9.1 Learning goals
- Know which artifact of evidence answers which audit question.
- Be able to reconstruct "who/what changed what, when, and with what approval".
- Know what an artifact deletion looks like in the audit log.

## 9.2 Core concepts

### 9.2.1 Evidence map
| Question | Where to look |
|---|---|
| Who asked Copilot to do it, when did it start/finish? | **PR timeline** ("Copilot started work", "Copilot finished work", assign/unassign, review requests) |
| What did the agent actually do (commands, edits, reasoning)? | **Session logs** (View session / Agents page); CLI session history; `/share` export |
| What did CI do with the change? | **Workflow run logs** (step logs, annotations, `$GITHUB_STEP_SUMMARY`) |
| What files/reports did automation produce? | **Workflow artifacts** (retention-limited!) |
| Who approved the run / merge? | PR reviews + the **Approve and run workflows** action in the PR/Actions history |
| Who changed settings, deleted artifacts, edited agents/policies? | **Audit log** (org/enterprise) |
| What code changed? | Commits (Copilot commits are co-authored with the requesting user), PR diff |

### 9.2.2 Audit log
- Org/enterprise audit log records administrative and security-relevant events (settings, policies, tokens, Actions changes, deletions…). Filter with `action:` qualifiers, e.g. `action:artifact.destroy`.
- **`artifact.destroy`** = an Actions **workflow artifact was deleted** (manually via UI/API, ⚠️ *verify whether retention-expiry cleanup is logged*). This is the event to look for when evidence "disappeared".
- Query examples:
  - UI: Org settings → Audit log → search `action:artifact.destroy`.
  - REST: `GET /orgs/{org}/audit-log?phrase=action:artifact.destroy`.
  - Streaming/export to SIEM for long-term retention.
- Retention (⚠️ *verify*): events are kept ~180 days in the UI/API; Git events much shorter (7 days) — stream/export if you need more.

### 9.2.3 Making agent work auditable by design
1. **Everything in a PR**: agents work through PRs (draft, human approval, timeline events).
2. **Persist logs as artifacts**: upload CLI/agent logs and findings with unique names and a **retention** that matches policy.
3. **Write verdicts to `$GITHUB_STEP_SUMMARY`** so the run page documents decisions.
4. **Least-privilege `permissions`** per job, so the log shows exactly who had write rights.
5. **Hooks**: `preToolUse`/`postToolUse` append every tool call to a log (Chapter 8).
6. **`concurrency` groups** keep the history linear.
7. **Don't rely on artifacts alone**: they expire and can be deleted (→ `artifact.destroy`).

### 9.2.4 Log types compared
| Log | Granularity | Lifetime | Tamper-resistance |
|---|---|---|---|
| PR timeline | Event-level | Lives with the PR | Edits to comments are shown; events immutable |
| Session log | Tool-call-level | Tied to session | Read-only in UI |
| Workflow log | Step-level | Retention setting (default ~90 days) | Can be deleted by admins (audit-logged) |
| Artifact | File-level | `retention-days` | Deletable → `artifact.destroy` |
| Audit log | Admin/security events | ~180 days (stream for more) | Not user-editable |

## 9.3 Exam traps
- "Artifacts are permanent" → no: `retention-days`, manual deletion.
- "Session log = workflow log" → different things: session log is the **agent's** activity; workflow log is the **Actions job's** output.
- "Audit log shows each command Copilot ran" → no, that's the **session log**.
- "Deleting an artifact leaves no trace" → it creates `artifact.destroy`.
- "Timeline shows tool calls" → it shows lifecycle events.

## 9.4 Hands-on lab
1. Run a Copilot task; list evidence: timeline events, session log, workflow log, artifact.
2. Delete the artifact from the run page; then (as org owner) find `artifact.destroy` in the audit log with a `phrase` filter or via REST.
3. Add `retention-days: 1` to an upload step and understand what changes.
4. Add a `$GITHUB_STEP_SUMMARY` block summarizing an auditor's verdict.

## 9.5 Self-check
1. Which audit event signals a deleted workflow artifact?
2. Which log shows an agent's commands?
3. Where do you see who started the Copilot task?
4. Name two controls that preserve agent evidence beyond default logs.
5. Why isn't an artifact sufficient as the only audit record?

<details><summary>Answers</summary>

1. `artifact.destroy`.
2. The agent session log.
3. PR timeline (and the issue/assignment events).
4. e.g. audit-log streaming, uploading logs as artifacts with suitable retention, hooks writing to a log.
5. It expires and can be deleted.
</details>

---

# Appendix A — One-page cheat sheet

**Contexts:** `head_ref`/`base_ref` only on PR events · `sha` on PR = merge commit · `run_id`, `run_number` stable on re-run, `run_attempt` increments.
**Outputs:** step `id` → `echo "k=v" >> "$GITHUB_OUTPUT"` → job `outputs:` → `needs.<job>.outputs.k`.
**Matrix from JSON:** `${{ fromJSON(needs.plan.outputs.list) }}`.
**Concurrency:** `group` + `cancel-in-progress` (true = kill running; false = queue, newest pending wins).
**CLI:** `-p` one-shot · `--agent` · `--autopilot` (keep going) · `--no-ask-user` (no questions) · `--continue` (latest) · `--resume` (pick/ID) · `--connect` (remote; conflicts with both) · `/delegate` (cloud + draft PR) · `/fleet` (parallel subagents) · `--disable-mcp-server X` / `--disable-builtin-mcps`.
**Cloud agent:** issue → 👀 → `copilot/` branch + draft PR → "started work" → **View session** → "finished work" → review; `@copilot` on PR; unassign/reassign if stuck (1 h timeout); **Approve and run workflows**.
**Files:** `.github/agents/*.agent.md` · `.github/prompts/*.prompt.md` · `.github/instructions/*.instructions.md` · `.github/skills/<n>/SKILL.md` · `.github/hooks/*.json`.
**Frontmatter:** `name` (opt) · `description` (req) · `tools` · `mcp-servers` · `target` · `model` · `user-invocable` · `disable-model-invocation` · (`infer` deprecated).
**Tools:** `read` · `search`(repo) · `edit` · `execute` · `agent` · `web`(fetch) · `todo`.
**MCP:** YAML `mcp-servers` / JSON `mcpServers` · `server/tool` vs `server/*` · `command`+`args` = local/stdio · `url` = http/sse · MCP allowlist ≠ firewall allowlist · secrets `COPILOT_MCP_*`.
**Hooks:** JSON v1 in `.github/hooks/` · `preToolUse` (CLI + cloud, can block) · `permissionRequest` (CLI only, before permission service) · SDK `onPreToolUse` / `onPostToolUse` (can modify result).
**Audit:** PR timeline · session logs · workflow logs · artifacts (expire) · audit log (`artifact.destroy`).

# Appendix B — Final 20-question mixed drill

1. A PR workflow prints an empty `github.head_ref`. Which event was it? *(Not a PR event, e.g. push.)*
2. Which context value changes on re-run? *(`run_attempt`.)*
3. Job `b` needs job `a`'s output `v`. Expression? *(`needs.a.outputs.v`.)*
4. Which step setting is mandatory for `steps.x.outputs.y` to work? *(`id: x`.)*
5. What does `cancel-in-progress: true` cancel? *(Runs already in progress in the same group.)*
6. Which alias is repo search? *(`search`.)*
7. Which alias fetches URLs? *(`web`.)*
8. Which key configures MCP in agent YAML? *(`mcp-servers`.)*
9. `url` + `headers` implies which transports? *(`http`/`sse`.)*
10. Which flag resumes most recent session? *(`--continue`.)*
11. Which CLI-only hook short-circuits permissions? *(`permissionRequest`.)*
12. How do you hide an agent from the picker but keep subagent use? *(`user-invocable: false`.)*
13. Which command hands a CLI task to the cloud? *(`/delegate`.)*
14. Which command parallelizes subagents? *(`/fleet`.)*
15. Button to run CI on a Copilot PR? *(Approve and run workflows.)*
16. Fix for stuck cloud-agent work? *(Unassign/reassign; `@copilot` for comments.)*
17. Audit event for deleted artifacts? *(`artifact.destroy`.)*
18. Where are hooks defined? *(`.github/hooks/*.json`.)*
19. Which flag prevents clarifying questions? *(`--no-ask-user`.)*
20. Which `tools` value means "none"? *(`[]`.)*
