# Liquido - Liquid Democracy

## A modern aproach to interactive representative direct democracy

Most people take the current political system in their country for granted. But is thas not always been like that. Modern political systems such as "Democracy" have been invented in ancient Greek.


## Suggest a (draft) law

Everybody is allowed to propose a new law. Before any proposal can become a draft law, it must accumulate a certain minimum amount of supporters. During this phase also the exact formulation of the proposed law must be agreed upon. It may also happen, (actually most probably it will happen), that opposing suggestions are also created. This is the normal process of discussion.

Once the proposals have reached the necessary form they can be voted upon.

## Preferential voting of proposals

You don't just simply vote for or against one proposal, but instead you sort the list of competing proposals into your personally preferred order. You do not need to support every proposal. It is perfectly ok to not support one specific proposal at all. But you should support at least one suggestion, ie. sort it into your list, otherwise you abstain your vote.

## Proxy voting / Delegated voting

Sometimes political descisions can by quite complex. Maybe you know someone who is an expert in that given area. In Liquid Democracy you may delegate your own vote to this expert, which is then called your 'proxy'. Your proxy can then vote with two vots, your one and his own one.

You only give a proxy the right to vote for you in one specific 'area', eg. family & social, finance, foreign policy, etc. You may choose different proxies for different areas. 

You can revoke your delegation at any time and then (hopefully) vote for yourself. You can decide this individually for every single proposal or area at any time: Vote yourself or trust your proxy.

Vote delegation can be transitive. This means that a proxy may re-delegate his collected rights (together with his own one) to another 'parent' proxy ... and so on. So in the end a tree of proxies forms where delegated rights accumulate at the top. (This is where political parties sit in todays democracy.) Circular delegation must be prevented and is forbidden!

Keep in mind that in Liquid Democracy everyone in the delegation chain may always choose to vote for himself. As a consequence his own right to vote (and the ones delegated to him if any) are revoked from the parent proxy. Revoking your delegationis is allowed for one specific proposal, for a given area or forever. As you choose.


## Process Flow

Adapted from process used in the software aula.de by Marina Weisband

![](./Liquido Flowchart.png)

 0. Setup: Initially some **areas of disussion** are created, e.g. topics, categories or departments.
 1. Everyone can **add an idea** no matter how unripe the idea might
    be. An idea is filed in one area and can be linked with other ideas.
 2. If enough users like to discuss an idea, then it is **moved onto the table** and it 
    becomes a proposal for a law.
 3. **Elaboration phase:** The proposals is discussed and can be improved. 
    Users can **suggest improvements**. And these suggestions can be voted up or down 
    by other users.
    In this phase the content of the proposal can still by changed by the original author. He 
    would be wise to adapt his proposal according to the most highly rated suggestions for 
    improvement in order to gain the highest ranking during the voting phase.
 4. During the elaboration phase **competing proposals** can also be created. Again a new 
    competing proposal must first find a minimum number of supporters before it is moved 
    onto the table.
 5. The **Voting phase** starts n days after the initial proposal reached its quorum.
    It will run over a defined period of time. Users don't just vote for or 
    against one proposal, but instead, each voter sorts (some or all of) the proposals into 
    his personally preferred order.   
 6. When the voting phase is finished, then the winning propsal becomes a real **law** - at least for a 
    defined minimum time.
 7. But voters may still change their mind after that. If a standing law looses support
    from more than 50% of its original supporters for more than n consecutive days, then 
    the law is repealed.

## Features of Liquido

 - The primary rule of a free democracy is that **votes are equal, secret and free.** And so they are in Liquido.
 - Votes can sort the list of competing into their personally preferred order. The final winner of the vote is calculated with the Schulze Method.
 - Privacy is a first order citizen in Liquido. The principle of privacy is enforced even against attacks from the inside.
 - Ballots are anonymous. A ballot cannot be associated with the voting user from the outside. (So voters cannot sell their vote.) 
   But voters can still privately change *their own* ballot at a later time. This is made possible by some very clever usage of crypto-hashes.

#### Delegated Voting via Proxies

 - Voters can delegate their right to vote to a proxy. A proxy can be chosen per area.
 - Any delegation can be retracted at any time.
 - Delegations can transitively be forwarded: A proxy can forward the rights to voted he received including with his own one to another proxy and so on. This way a tree of proxies forms where the votes accumulate at the top.
 - Voters can see their own private chain of proxies up to their final delegate. But the full tree of proxies is private.
 - A voter gets informed about the vote that his proxy casts in an area. But only delegees get informed. Other than that the voting decision of any voter or proxy is private.
 - A voter can always overrule the decision of his proxy. Even when the proxy has already casted his vote (and the voting phase for a law is still ongoing).
 
#### Lifetime of a law

 - Ideas need to reach a certain quorum of supporters before they can be discussed.
 - Competing proposals need the same minimum quorum before they are include in the vote.
 - Laws can be repealed if they continuusly loose support over a consecutive number of days.

#### Configurability

 - Parameters that can be configured:
   - Quorum: Minimum number of supporters that are necessary for an idea (or a competing proposal) to become a proposal that will be voted upon.
   - Timespan how long an initial law will be elaborated before voting starts.
   - Timespan for voting phase. 
   - Both timespans can be configured individually per law.
 - Liquido can be configured, so that transitive forwarding of a vote can be forbidden by the delegating user. That means that a user may delegate his voice to his proxy. But the proxy cannot re-delegate this voice again.


# Technical Architecture

## Secure electronic voting

 - Each voter can request a voterToken for an area from the Server. The voterToken is the digital right to vote in that area.
   The voterToken is strictly confidential. Only the user must know his own voterToken.
   When a user receives a voterToken, then checksum of that voterToken is also stored on the server.
   voterToken = hash(user.email, user.password, area.id secretSalt)
   voterTokenChecksum = hash(voterToken)

 - With this voterToken a use can request a pollToken. The pollToken is a one time password that can be used to 
   cast a vote in one poll. 

 - When the user assigns a proxy in one area, then this proxy has the right to vote in place of that user.
   (But remember that a user can always vote for himself. Even when the proxy has already voted. As long as the poll is in its voting phase.)
   The proxy receives a proxyToken for that area. proxyToken = hash(voterToken, proxy.id, area.id)
   The proxyToken checksum is also added to the list of valid voterTokenChecksums.
 

    voterToken           = hash(user.email, user.password, area.id, secretSalt)       // only known to the voter himself
    voterTokenChecksum   = hash(voterToken, secretSalt)                               // stored on the server
   
    proxyToken           = hash(voterToken, user.id, proxy.id, area.id)               // right to cast a vote for delegee in that area
    proxyTokenChecksum   = hash(proxyToken, secretSalt)                               // stored on the server

    pollToken            = hash(voterToken or proxytoken, poll.id, secretSalt)        // right to vote anonymously in this poll

    addProxy    := create proxyToken from usersVoter Token on the server and store it. (proxy must not know voterToken!)
    removeProxy := delete proxyTokenChechsum from list of valid tokenChecksums


#### Technial Implementation Features

 - Clean model-view-controller implementation in JavaScript
 - Modern HTML5 bootstrap frontend with great emphaasis on usability 
 - Powerfull NodeJS/express backend with REST API
 - Performant MongoDB database
 - High Security: HTTPS/TLS encryption, cross site forgery requests are prevented, paswsords are never directly stored
 - Well documented code
 - Open source licence

#### Open Feature decisions:

 - Shall it be possible for a proxy to reject a delegation? (This may be necessary, because your delegees can see your vote.)
 - Should areas be hierarchic?
 - How to prevent too many competing proposals? (Is that actually a problem?)



## Functional data model
*(Classes and attributes)* Every class has the fields ID, createdAt, modifiedAt

#### Area (Department)
 - Title
 - Description
 - ParentAreaId (?)

#### Citizen (User/Voter)
 - EMail
 - Credentials: Password-Hash
 - Proxy (per Area)

#### (Proposal for a) Law
 - Title
 - Description
 - Creator
 - AreaID
 - Status 
     - proposed: suggestion for a new law, suggestion may still be edited, is waiting for supporters, competing suggestions may be linked
     - draft: found the necessary amount of supporters, poll can start
     - poll: poll is currently in progress and users can cast their votes (ie. order the competing proposals)
     - accepted: proposal has been accepted as a real law
     - not_accepted: proposal did not get enough votes
     - loosing_support: law has currenlty not enough supportes to stay a law
     - repealed: law was discarded, because is was loosing_support for longer than x days.
 

#### Competing proposals (Opposing suggestions, 1:n)

  - InitialLawId
  - CompetingLawId

#### Ballot of a voter ("Stimmzettel")
 - PK: BallotId
 - PK: VoterToken  (Secret Hashed Value)
 - PK: InitialLawId
 - VoteOrder: 
     Sorted list of competing law ids that are supported by this user (in this order) 
     (Not all competitors must be contained in that list.)
     (List may or may not include InitialLawId.)
 - votedDirectly? (or through delegation from proxy)
 - votingProxyId: User at the end of the proxy chain, that actually voted  (non normalized field for optimization)


## Frontend View
*(Design: How will HTML pages look like?)*

### Startpage
Offers some information for first time visitors and directs them to some general information about Liquid Democracy.
Offers some very general statistics about current vots and laws.

### User Page

Show things this user can *do now* :

 - propose a completely new law (button)
 - support a proposed law draft (list of newly created proposals)
 - vote in a currently open poll (list of open polls with nice "timeline" view about how much time is left to vote in this poll, emphasize votes that will close soon.)

List of currently open polls is a sortable and filterable table. Colums are Attributes from Class "Law": Title, Creator, Area, Status and Description. Each row links to View one Law page.

### Create new proposal page



=> Automatically offer similar proposals and link to them as competing suggestion before saving new proposal. (Tip: Better chance to get enough supporters!)

## Page Flow

    Startpage -> Login  -> UserHome 
    
    UserHome    -> Actions: Add new idea
    (list of    
     panels)
     
    Panels -> Idea
              Action: "like to discuss this"
           -> Proposed Law
              Action: Show details (incl. competing proposals)
           -> Poll (Currently Running)
              Action: Vote (=sort proposals)   
                      
                      
    View a proposed law (in Status Draft) -> 
      Timeline/Overview
      Title, Description
      Statistics: Last updated by author, number of supporters, "activity index"
      Actions: 
        - Suggest improvement
        - Up/Down vote improvement
        - Add competing proposal
        
            





## Business Rules (Behaviour Driven Design)
*(Think of automated tests*)

WHEN a proposal has at least n users that support it 
THEN it can become a poll.

GIVEN a citizen that wants to delegate his right to vote to a proxy
WHEN  this delegation would create a circular delegation
THEN  it is not allowed.
=> Explanation: All the citizens in the circle would not vote, although they most probably might not expect that.

GIVEN a poll that is started
WHEN  a user wants to cast his vote
THEN  he can sort the opposing suggestions into his personally preferred order.

GIVEN a proxy that has the right to vote for many users (maybe even over several levels of proxiing)
WHEN  this proxy casts a vote, ie. sorts competing proposals into an order
THEN  this vote counts for every user that (directly or indirectly) gave his right to vote to this proxy
 AND  this vote is stored under every UserId.

GIVEN a user that wants to vote for himself
WHEN  this user revokes his right to vote from his proxy
THEN  this user's (and all his proxee's) vote orders are cleared
 AND  he can order proposals on his own.

GIVEN a poll that is close to its end
WHEN  a last proxy in the delegation chain has not yet voted
THEN  all members of the chain shall get notified
 AND  they can decide to vote on their own.
=> Or simpler: Always inform any citizen that has not voted yet on a closing poll (either trough delegation or on his own.)



