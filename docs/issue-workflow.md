# Issues, priorities and how work is ordered

Developer documentation for `mystic-crypt-ui`: what the labels on an issue mean, how the
queue is ordered, and which decisions belong in an issue rather than in a commit.

The binding short form lives in `.claude/rules/ai-workflow/github-issue-policy.md`. This page
is the long form: the same rules with the reasoning and the cases that produced them.

---

## 1. Every fix has an issue first

An issue exists before the fix begins, and the commit and pull request cite it. Not because a
number is valuable, but because the issue is where the question gets asked and the decision
gets recorded. A fix without one leaves the reasoning in a commit body, where nobody looks
for it a year later.

A bug found while fixing another bug gets its own issue before its own fix. And if a
pre-implementation check shows the reported defect does not exist, that finding is what gets
written down - a false issue is worse than no issue.

---

## 2. The priority scale

Priority follows **damage**, not effort. A one-line fix for a defect that loses data outranks
a week of work on a confusing dialog. Sorting by effort is what turns the label into a
negotiation at the next issue.

| Label | Meaning | The test for it |
|---|---|---|
| `P0` | a released version is losing or exposing data right now | a release goes out **today** because of this |
| `P1` | data loss, security, or a gate that cannot report | the next release does not go out without this |
| `P2` | important, scheduled next | it is in the plan, not in this release |
| `P3` | small or cosmetic | it confuses somebody; it destroys nothing |
| `P4` | worth doing, nobody is waiting | picked up when something else touches that area |
| `P5` | recorded so it is not rediscovered | nobody will do it, and the issue says why |

**The only line that interrupts work in progress is P0 against P1.** Everything else reorders
a queue; P0 empties it.

Two labels carry a condition rather than a stage:

- `blocked` waits for a **named** event, written in the issue. #301 waits for the second
  plugin that opts into the public state. A condition carried as `P3` turns "not yet" into
  "some day", which is why it is not a stage.
- `P5` is the honest form of `wontfix`: the issue stays, with the reason. If `P4` or `P5`
  becomes a place where things are forgotten quietly, it is doing `wontfix`'s job badly and
  gets dropped.

### Worked examples from this repository

| Issue | Label | Why |
|---|---|---|
| #300 Save As replaces an existing vault without asking | `P1` | a mis-click in a file chooser destroys somebody's database |
| #303 an edit in an open dialog is lost to the automatic lock | `P1` | ordinary use, no attacker, the work is gone |
| #306 CI does not report what it ran | `P2` | every test count in a pull request depends on it |
| #297 the conversion wizard does not show the source file | `P3` | it confuses; nothing is lost |
| #309 two documents are in German | `P3` | a reader is inconvenienced, nothing breaks |

---

## 3. A decision an issue reserves is taken in the issue

When an issue names a question as open - "to be decided", "not to implement blindly", a
choice listed for the maintainer - the answer is written **in the issue**, before the code
that depends on it.

It is not made inside the commit that implements it, however reasonable the choice. #272
named the legacy-entry question as open; it was answered inside the implementing commit, and
the issue had no comments at all. The choice was sound and nobody could see that a question
had ever been asked.

If it was already decided in a commit: the commit stands, nothing is reverted, and the
decision is made again - visibly - in the issue: what was built, what the alternatives were,
and the question put to the maintainer.

---

## 4. The queue

On "work through the issues", the open issues are the queue, ordered by label first and by
smallest scope within a tier. For each: the reproducing test first, then the fix, in one
commit that cites the issue, then a pull request against `develop`.

A merge goes through the pull request. Pushing a branch head onto `develop` puts the content
there while the pull request stays open and unmerged - no CI run on the merged state, and no
record that the change arrived through review.

---

## 5. What closes an issue

`Closes #NN` in the commit or the pull request body, so the merge closes it. Closing by hand
is how an issue gets closed for work that never landed.

After a merge, two things are checked, not one: that the issue is closed **and** that the
expected files are on the target branch. A squash freezes the branch at merge time, so a push
made after the merge is silently lost while everything reads as success.
