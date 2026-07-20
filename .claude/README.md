# Claude Code documentation kit

A subagent + skill pair that write code comments, Javadoc, and spec docs (README/API spec/ADR/PRD) for a Java/Spring Boot codebase, kept in sync with the code as you write it.

## Install

Drop these into your repo (project-scoped, so check them into git and your team gets them too):

```
your-repo/
  .claude/
    agents/
      doc-writer.md          <- from agents/doc-writer.md
    skills/
      documentation-standards/
        SKILL.md              <- from skills/documentation-standards/SKILL.md
        references/
          comment-examples.md
          spec-templates.md
```

```bash
mkdir -p .claude/agents .claude/skills
cp agents/doc-writer.md .claude/agents/
cp -r skills/documentation-standards .claude/skills/
```

Restart Claude Code once if `.claude/agents/` or `.claude/skills/` didn't already exist in this project — the file watcher only picks up new directories on a fresh session.

## How the two pieces relate

- **`documentation-standards`** is the knowledge: comment philosophy, Javadoc rules, Spring Boot conventions, and templates for ADR/PRD/API-spec/README.
- **`doc-writer`** is the worker: a subagent that reads whatever you just changed and applies those conventions. Its frontmatter preloads the skill (`skills: [documentation-standards]`), so it never has to "discover" the rules mid-task — it starts with them already in context.

`documentation-standards` also works standalone: Claude will pull it in on the main thread any time you ask it to document something directly, without going through the subagent.

## Getting "parallel with the code I write"

Claude Code delegates to `doc-writer` automatically when its description matches what you're doing — that's what the "Use PROACTIVELY... immediately after implementing or modifying a class, service, REST controller..." phrasing in its `description` field is for. In practice:

- **After you ask Claude to implement something**, it will often invoke `doc-writer` on its own once the implementation is done, since the description explicitly tells it to.
- **To guarantee it every time**, just say so once per session: *"After every change, use the doc-writer subagent to document it before moving on."* Claude will then chain it after each implementation step for the rest of the session.
- **On demand**, `@doc-writer document the changes in src/rate-cards/` (or just "use doc-writer on this") forces it regardless of automatic delegation.

### Optional: enforce it with a hook instead of relying on the model

If you want this to be non-negotiable rather than a proactive-but-skippable behavior, add a `PostToolUse` hook in `.claude/settings.json` that fires after `Edit`/`Write` on source files and tells Claude to delegate — this is deterministic where the agent description is only a strong hint:

```json
{
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Edit|Write",
        "hooks": [
          {
            "type": "command",
            "command": "echo 'Reminder: use the doc-writer subagent to document this change before finishing.' >&2; exit 2"
          }
        ]
      }
    ]
  }
}
```

This is blunt — it fires on *every* edit, including test files and docs themselves, so it's worth scoping the matcher or the script further before turning it on. Start without it; add it only if you find Claude skipping documentation in practice.

## Design choices worth knowing

- `doc-writer` can `Read`/`Write`/`Edit`, but its system prompt draws a hard line: it only ever touches comments and doc files, never logic, signatures, or behavior. If you want it to also *fix* bugs it notices while documenting, that's a deliberate scope decision to revisit, not an oversight — bundling "document" and "fix" into one agent makes its output harder to review at a glance.
- It reports back a summary, not full file contents, to keep your main conversation's context clean — the same reason subagents exist at all.
- Spec-doc templates live in `references/`, not inline in `SKILL.md`, per Anthropic's own guidance to keep the always-loaded description small and push detail into files the skill only reads when relevant.
