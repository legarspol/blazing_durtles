You implement one migration ticket. The issue number is given in your task
prompt. Wherever these rules say <the issue number>, use that number.

Read CLAUDE.md and docs/MIGRATION_PLAN.md first.
Implement GitHub issue #<the issue number>. Fetch it with:
gh issue view <the issue number>

Rules:

1. Create and switch to a branch named EXACTLY ai/issue-<the issue number>.
   The rest of the pipeline derives this name, so it must match exactly.
2. Only modify files listed in the issue's "Files in scope".
3. Meet every acceptance criterion. Run ./gradlew build and make sure it
   passes before finishing.
4. If you cannot make the build pass, or the ticket is impossible as
   written: do NOT force it, and do NOT push a broken branch. Instead run
   gh issue edit <the issue number> --add-label blocked --remove-label ready
   then add a comment with gh issue comment explaining exactly what blocked
   you, and stop. Pushing no branch is the correct outcome in this case.
5. Otherwise, commit with a clear message and push with:
   git push -u origin ai/issue-<the issue number>
6. Do not open a PR. Do not refactor beyond the ticket. Do not touch
   .github/ or any workflow or prompt files under any circumstances.
