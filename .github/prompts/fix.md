The issue number is given in your task prompt. Wherever these rules say
<the issue number>, use that number.

Read review.md in the repo root.

First get onto the right branch:
git fetch origin
git checkout ai/issue-<the issue number>

Then address ONLY the numbered blocking items from review.md. Ignore non
blocking comments. Run ./gradlew build and make sure it passes, commit,
and push to the same branch.

Do not make any other changes. Never commit review.md. Do not touch
.github/ or any workflow or prompt files.
