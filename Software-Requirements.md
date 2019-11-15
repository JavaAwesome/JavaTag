# Software Requirements

## Vision
To create an engaging, fun, and high-tech Android app that enables users to create custom games of Tag with their friends.

## Scope (In/Out)
### In
Our app will provide the following functionality in the MVP:
* Multiple users
* Functionality to 'tag' other users
* Notifications when users are tagged
* A map view of the game players

### Out
Our app will not provide the following functionality in the MVP:
* This app will never hold personal biometrics such as facial recognition on our server
* This app will never devulge a user's location to members not in the current game group
* This app will never sell a user's location to outside vendors

## MVP
Our MVP will include:
* At least two total players (one other player) 
* Possible tag definitions, may change as we go:
    * By distance
    * By QR Code 
    * By taking a picture of them
    * By facial recognition 
* Tag notifications

## Stretch
Our stretch goals include:
* Showing multiple players on a map
* Geo fence (bounds)
* Control who can see the game by using authentication
* Bread crumbs
* Power ups
* Themed games
* Ability to have multiple games running
* Ability to customize who is invited to a game
* Customize notifications when tagged
* Have a stats page that shows metrics including how far user has walked, how many times user has been tagged
* Bonus items to find or purchase that give you extra powers like improved visibility, increased frequency of location updates, cloaking
* Monetization

## Functional Requirements
Functional requirements of our MVP include:
1.	A user can view the location of all members in the current game group
2.	A user be notified when they have been tagged and when they have tagged someone else
3.	A user can log in and log out
4. A user's map view will follow their movement during the game

## Non-Functional Requirements
Non-Functional requirements of our MVP include:
1.	A user’s password will be protected using Amplify Cognito
2. The app will be tested using Espresso

## Data Flow 
A user opens the app and signs in. If there is no active game a new game will start when there are at least two users signed in. The user leaves the game by signing out of the app.

![Data Flow Sketch]()
