# Productivity & Planning — CMS Console Interaction Specification

## 1. Inbox & Today

**Route:** `/console/planning/today`

### Header
- Title `Inbox & Today`.
- Primary `Add task`.
- Secondary `Plan day`.
- Date control defaults to Today; arrows move previous/next day.

### Page layout
Desktop uses two columns: left 40% Inbox, right 60% Today timeline/list. Mobile stacks Inbox then Today.

### Inbox panel
Rows contain:
- completion checkbox;
- task title;
- optional source/resource chip;
- capture time;
- urgency/importance indicators;
- overflow.

Inline interactions:
- Checkbox marks complete with Undo Snackbar.
- Clicking title opens task detail side sheet.
- `Plan` button/drag moves task into Today and asks for time block only if scheduling is desired.

Quick capture field at top accepts title on Enter. Optional natural-language parsing may suggest date/project but must preview parsed fields before committing hidden changes.

### Today panel
Sections: Overdue, Scheduled, Unscheduled, Completed collapsed by default.

Task row fields: status, title, project, scheduled time, deadline, estimate, actual/focus duration, linked resource, priority indicators.

Drag between sections updates scheduling/status only after valid drop. If a drop creates time conflict, show conflict sheet with `Keep both`, `Move`, `Cancel`.

## 2. Projects & Tasks

**Route:** `/console/planning/projects`

Left project navigation panel: All Tasks, Active Projects, Someday, Archived. Project rows show name and incomplete count.

Main header shows selected project name, status chip, owner, progress. Actions: Add task, Edit project, overflow.

Views: List, Board, Timeline when supported. View selection persists per project.

### Task list columns
- status checkbox;
- title;
- assignee/owner when multi-user;
- importance/urgency;
- scheduled;
- deadline;
- estimate;
- linked resource;
- updated;
- actions.

### Task detail side sheet
Sections:
- title and status;
- project;
- description;
- assignee/owner;
- priority/importance/urgency;
- schedule and deadline;
- estimate and actual time;
- linked Resources/Collections;
- checklist/subtasks;
- tags;
- activity/comments where enabled.

Changing status saves immediately except transitions with business validation. Destructive `Delete task` appears in overflow and confirms that linked Resource is unaffected.

### Project editor
Fields: name required, description, status, start date, target date, color/icon, default collection/resource links, members/permissions if supported. Archive keeps tasks and history; Delete requires dependency summary.

## 3. Calendar & Time Blocks

**Route:** `/console/planning/calendar`

### Header controls
Today button, previous/next arrows, date range label, view segmented control (`Day`, `Week`, `Month`), `New time block`.

### Calendar canvas
Day/week show time grid. Month shows compact task/event summaries.

Time block visual contains title, time range, linked task/project icon, conflict indicator, completion/focus state.

Interactions:
- Click empty slot opens create sheet prefilled with start time.
- Drag block moves it; resize handles change duration.
- Before persistence, show temporary ghost position.
- Conflicts are visually marked; hard conflicts defined by policy require confirmation.
- Clicking block opens detail popover/sheet.

Create/edit fields: title, start/end, timezone, linked task, linked Resource, recurrence, reminder, notes. End must be after start. Recurrence editor supports preview of next occurrences.

## 4. Goals & OKR

**Route:** `/console/planning/goals`

Tabs: Goals, OKR, Archive.

Goal cards show title, period, progress, confidence/status, linked projects/tasks, owner. Filter by period/status/owner.

Goal detail:
- summary card;
- progress chart;
- milestones/key results;
- linked projects/tasks;
- check-in history.

Create goal fields: title, description, measurement method (`manual`, `task completion`, `numeric`), starting/current/target values when numeric, period, owner, linked entities.

OKR key-result rows show target, current, unit, progress, last check-in. `Check in` dialog asks current value, confidence, note; history remains immutable except admin correction with audit.

## 5. Habits, Focus & Review

**Route:** `/console/planning/focus`

Tabs: Habits, Focus Sessions, Review.

### Habits
Table/cards: habit name, cadence, current streak, completion rate, today state, next due, actions.

Habit editor: name, cadence, target frequency, reminder, start/end, linked goal. Completing today toggles with Undo and updates streak after server confirmation.

### Focus Sessions
Top card provides timer/session controls only if server/client architecture supports authoritative session state. Fields: linked task, planned duration, mode, interruption notes.

History table: task, planned, actual, started, ended, interruptions, outcome.

### Review
Period selector: Daily, Weekly, Monthly. Review page composes read-only summary cards: completed tasks, planned vs actual time, overdue carryover, habits, goal progress, notable activity. Editable reflection fields: wins, blockers, notes, next priorities. Saving review creates versioned review record; prior periods remain accessible.

## Shared states
- Time/date operations always display timezone where ambiguity exists.
- Task updates are optimistic only for reversible low-risk fields; scheduling conflicts and status transitions wait for server response.
- Completed items can be hidden but never silently deleted.
- Calendar and planning filters are URL-addressable so a filtered view can be bookmarked.
