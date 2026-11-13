# Bugfix-only until the RELEASE-8.15 tag

The maintainer's decision of 2026-11-13: develop takes only fix, test, docs, build and chore
until 8.15 is tagged; `scripts/merge_pr.py` refuses a feat pull request while this file lies
here. The tag removes this file. The cut list is the 8.15 milestone.
