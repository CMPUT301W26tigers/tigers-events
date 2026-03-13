This is a group project for a class where we are tasked with designing and creating a mobile app using android studio in java. We collaborate using github. 
This app is a simple event organization system for a recreation centre, users can host events by creating an event object that contains the details for the event. The event is then posted on the app where other users can join the event, however there is a twist. When users click the join event button, they are added to the events waitlist. Users are then chosen from the waitlist via a lottery system and are given a notification in their inbox, and are either bumped up to an accepted list, or remain in the waitlist.
The app uses fragments to implement a bottom navigation bar with 3 tabs: Explore, events account. Explore is a UI what contains event objects displayed in cards, as well as a search bar that will filter the events that show up. The event UI is simply a screen that shows events that the user has signed up for. The account tab shows the users information as well as the ability to make changes to thier information. On the top of the screen, there is an inbox button that when pressed displays all of the users notifications.
I want you to only implement what i ask and make minimal changes that satisfy what I ask. Make test cases for github actions.

## PR & Branch Conventions

- Branch naming: `Adrian/<FeatureName>` (e.g. `Adrian/ExplorePageFix`)
- Leave committing, pushing, and PR creation to the user — do not do these automatically
- Provide a short summary of changes for the user to reference when writing their own description

## Workflow

- After making changes, merge them into the local main branch so they are immediately visible in Android Studio
- Do not push to remote or create PRs unless explicitly asked