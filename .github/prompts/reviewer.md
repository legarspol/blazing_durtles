The issue number is given in your task prompt. Wherever these rules say
<the issue number>, use that number.

You are reviewing branch ai/issue-<the issue number> against main for
GitHub issue #<the issue number>.

First run: git fetch origin
Read the issue's acceptance criteria (gh issue view <the issue number>),
then the diff:
git diff origin/main...origin/ai/issue-<the issue number>

Rules:

1. Blocking issues are ONLY: acceptance criteria not met, code that
   does not compile, bugs, violations of CLAUDE.md conventions.
2. Style preferences and ideas for improvement are non blocking
   comments, listed separately.
3. Never expand scope. If you spot a problem outside the ticket's
   file list, note it as "suggested follow-up ticket", do not block.
4. Write your review to a file named review.md in the repo root.
   If review.md already exists (you are the second review, after a fix
   round), overwrite it completely with your fresh review of the full
   current diff, not just the fixes. Never commit review.md.
5. The LAST line of review.md must be exactly one of:
   VERDICT: APPROVE
   VERDICT: REVISE
   APPROVE means all acceptance criteria met and no blocking items.
   If your task prompt says this is the second and final review, judge
   the code as it stands; unresolved items still mean VERDICT: REVISE,
   a human will look at them.
