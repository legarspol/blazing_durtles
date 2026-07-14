You implement one migration ticket. The issue number is given in your task
prompt. Wherever these rules say <the issue number>, use that number.

Read CLAUDE.md and docs/MIGRATION_PLAN.md first.
Implement GitHub issue #<the issue number>. Fetch it with:
gh issue view <the issue number>

Rules:

1. Create and switch to a branch named EXACTLY ai/issue-<the issue number>.
   The rest of the pipeline derives this name, so it must match exactly.
2. Only modify files listed in the issue's "Files in scope".
3. Meet every acceptance criterion. Run ./gradlew assembleDebug and make
   sure it passes before finishing. Do NOT run ./gradlew build: it adds
   release minification, lint and unit tests, which is far slower and can
   surface pre-existing failures that are unrelated to your ticket and not
   yours to fix. assembleDebug is the build gate (see CLAUDE.md).
4. Do not burn turns thrashing. Use well-known stable dependency versions;
   do not probe Maven/plugin repositories or read toolchain docs over many
   turns to discover version numbers. If assembleDebug still will not pass
   after a couple of focused attempts, the ticket is blocked, or it is
   impossible as written: do NOT force it, and do NOT push a broken branch.
   Instead run
   gh issue edit <the issue number> --add-label blocked --remove-label ready
   then add a comment with gh issue comment explaining exactly what blocked
   you, and stop. Pushing no branch is the correct outcome in this case.
5. Otherwise, commit with a clear message and push with:
   git push -u origin ai/issue-<the issue number>
6. Do not open a PR. Do not refactor beyond the ticket. Do not touch
   .github/ or any workflow or prompt files under any circumstances.
