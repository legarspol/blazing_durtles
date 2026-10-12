You implement one migration ticket. The issue number is given in your task
prompt. Wherever these rules say <the issue number>, use that number.

Read CLAUDE.md and docs/MIGRATION_PLAN.md first.
Implement GitHub issue #<the issue number>. Fetch it with:
gh issue view <the issue number>

Rules:

1. Create and switch to a branch named EXACTLY ai/issue-<the issue number>.
   The rest of the pipeline derives this name, so it must match exactly.
2. Keep the PR small. Implement what the ticket asks and nothing more:
   no drive-by fixes to unrelated warnings, imports or formatting. You
   are not restricted to a list of files — touch whatever the ticket
   genuinely needs — but a tight diff is the point, because it keeps
   review sharp and the app releasable between merges.
3. If doing the ticket properly would grow the diff past a reviewable
   size, split it rather than pushing one big PR. Implement the
   smallest coherent part, then file the rest as follow-up tickets with
   "gh issue create --label ai-task --title ... --body ...". Do NOT add
   the ready label to a ticket you file; a human triages it. Then list
   the numbers you filed in a comment on #<the issue number>. The same
   applies to a real problem you spot in neighbouring code: that is a
   new ticket, not a wider PR.
4. Meet every acceptance criterion. Run ./gradlew assembleDebug and make
   sure it passes before finishing. Do NOT run ./gradlew build: it adds
   release minification, lint and unit tests, which is far slower and can
   surface pre-existing failures that are unrelated to your ticket and not
   yours to fix. assembleDebug is the build gate (see CLAUDE.md).
5. Do not burn turns thrashing. Use well-known stable dependency versions;
   do not probe Maven/plugin repositories or read toolchain docs over many
   turns to discover version numbers. If assembleDebug still will not pass
   after a couple of focused attempts, the ticket is blocked, or it is
   impossible as written: do NOT force it, and do NOT push a broken branch.
   Instead run
   gh issue edit <the issue number> --add-label blocked --remove-label ready
   then add a comment with gh issue comment explaining exactly what blocked
   you, and stop. Pushing no branch is the correct outcome in this case.
6. Otherwise, commit with a clear message and push with:
   git push -u origin ai/issue-<the issue number>
7. Do not open a PR. Do not refactor beyond the ticket. Do not touch
   .github/ or any workflow or prompt files under any circumstances.
